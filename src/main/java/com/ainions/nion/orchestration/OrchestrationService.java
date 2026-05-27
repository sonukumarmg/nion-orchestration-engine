package com.ainions.nion.orchestration;

import com.ainions.nion.domain.AgentRun;
import com.ainions.nion.domain.EvaluationResult;
import com.ainions.nion.domain.OrchestrationRun;
import com.ainions.nion.domain.PlannedTask;
import com.ainions.nion.domain.enums.AgentStatus;
import com.ainions.nion.domain.enums.RunStatus;
import com.ainions.nion.document.RawMessageDocument;
import com.ainions.nion.notification.NotificationService;
import com.ainions.nion.orchestration.model.DomainResult;
import com.ainions.nion.orchestration.model.L1Plan;
import com.ainions.nion.orchestration.model.PlannedTaskModel;
import com.ainions.nion.repository.EvaluationResultRepository;
import com.ainions.nion.repository.OrchestrationRunRepository;
import com.ainions.nion.repository.RawMessageRepository;
import com.ainions.nion.tenant.TenantContext;
import com.ainions.nion.util.AuditLogService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationService.class);

    private final RawMessageRepository rawMessageRepository;
    private final OrchestrationRunRepository runRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final L1Planner l1Planner;
    private final L2Coordinator l2Coordinator;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final EvaluationService evaluationService;
    private final OrchestrationMapFormatter mapFormatter;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public OrchestrationService(
            RawMessageRepository rawMessageRepository,
            OrchestrationRunRepository runRepository,
            EvaluationResultRepository evaluationResultRepository,
            L1Planner l1Planner,
            L2Coordinator l2Coordinator,
            KnowledgeRetrievalService knowledgeRetrievalService,
            EvaluationService evaluationService,
            OrchestrationMapFormatter mapFormatter,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this.rawMessageRepository = rawMessageRepository;
        this.runRepository = runRepository;
        this.evaluationResultRepository = evaluationResultRepository;
        this.l1Planner = l1Planner;
        this.l2Coordinator = l2Coordinator;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.evaluationService = evaluationService;
        this.mapFormatter = mapFormatter;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UUID startRun(String source, String payload, String submittedBy) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        RawMessageDocument document = new RawMessageDocument();
        document.setTenantId(tenantId);
        document.setSource(source);
        document.setPayload(payload);
        document.setSubmittedBy(submittedBy);
        document.setReceivedAt(OffsetDateTime.now());
        RawMessageDocument saved = rawMessageRepository.save(document);

        OrchestrationRun run = new OrchestrationRun();
        run.setTenantId(tenantId);
        run.setRawMessageId(saved.getId());
        run.setMessageSource(source);
        run.setStatus(RunStatus.QUEUED);
        OrchestrationRun persisted = runRepository.save(run);
        auditLogService.logEvent(submittedBy, "RUN_CREATED", persisted.getId().toString(), "queued");
        orchestrateAsync(persisted.getId(), tenantId);
        return persisted.getId();
    }

    @Async("orchestrationExecutor")
    @Transactional
    public void orchestrateAsync(UUID runId, String tenantId) {
        TenantContext.setTenantId(tenantId);
        OrchestrationRun run = runRepository.findById(runId).orElseThrow();
        try {
            run.setStatus(RunStatus.RUNNING);
            run.setStartedAt(OffsetDateTime.now());

            RawMessageDocument document = rawMessageRepository.findById(run.getRawMessageId()).orElseThrow();
            String message = document.getPayload();
            String context = knowledgeRetrievalService.retrieveContext(message);

            L1Plan plan = l1Planner.plan(message, context);
            run.setIntent(plan.intent());
            run.setMessageType(plan.messageType());

            List<DomainResult> domainResults = new ArrayList<>();
            for (PlannedTaskModel task : plan.tasks()) {
                PlannedTask taskEntity = mapTask(task, run, tenantId);
                DomainResult domainResult = l2Coordinator.execute(task, message, context);
                domainResults.add(domainResult);
                run.getTasks().add(taskEntity);
                run.getAgentRuns().addAll(mapAgentRuns(domainResult, run, taskEntity, tenantId));
            }

            List<String> summaries = domainResults.stream().map(DomainResult::summary).toList();
            var evaluation = evaluationService.evaluate(message, summaries);
            EvaluationResult evaluationResult = mapEvaluation(evaluation, run, tenantId);
            evaluationResultRepository.save(evaluationResult);

            run.setSummary("Domains executed: " + domainResults.size());
            run.setMapOutput(mapFormatter.format(runId.toString(), plan, domainResults));
            run.setStatus(RunStatus.COMPLETED);
            run.setCompletedAt(OffsetDateTime.now());
            runRepository.save(run);

            notificationService.notifyCompletion(run, evaluation);
            auditLogService.logEvent("system", "RUN_COMPLETED", runId.toString(), evaluation.status().name());
        } catch (Exception ex) {
            log.error("Orchestration failed runId={}", runId, ex);
            run.setStatus(RunStatus.FAILED);
            run.setCompletedAt(OffsetDateTime.now());
            runRepository.save(run);
            auditLogService.logEvent("system", "RUN_FAILED", runId.toString(), ex.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private PlannedTask mapTask(PlannedTaskModel model, OrchestrationRun run, String tenantId) {
        PlannedTask task = new PlannedTask();
        task.setTenantId(tenantId);
        task.setTaskId(model.taskId());
        task.setDomain(model.domain());
        task.setPriority(model.priority());
        task.setRationale(model.rationale());
        task.setAgentHints(model.agentHints());
        task.setRun(run);
        return task;
    }

    private List<AgentRun> mapAgentRuns(DomainResult result, OrchestrationRun run, PlannedTask task, String tenantId) {
        List<AgentRun> agents = new ArrayList<>();
        result.agentResults().forEach(agentResult -> {
            AgentRun agentRun = new AgentRun();
            agentRun.setTenantId(tenantId);
            agentRun.setAgentName(agentResult.agent().name());
            agentRun.setStatus(agentResult.status());
            agentRun.setExecTimeMs(agentResult.execTimeMs());
            agentRun.setOutputPayload(agentResult.output());
            agentRun.setStartedAt(OffsetDateTime.now());
            agentRun.setCompletedAt(OffsetDateTime.now());
            agentRun.setRun(run);
            agentRun.setTask(task);
            agents.add(agentRun);
        });
        return agents;
    }

    private EvaluationResult mapEvaluation(com.ainions.nion.orchestration.model.CrossCuttingResult result,
                                         OrchestrationRun run,
                                         String tenantId) {
        EvaluationResult evaluationResult = new EvaluationResult();
        evaluationResult.setTenantId(tenantId);
        evaluationResult.setRun(run);
        evaluationResult.setToneOk(result.status() == AgentStatus.SUCCESS);
        evaluationResult.setAccuracyScore(8);
        evaluationResult.setCompletenessScore(7);
        evaluationResult.setOverallQuality(result.status().name());
        evaluationResult.setRecommendation("Review outputs for completeness.");
        evaluationResult.setRawOutput(result.output());
        return evaluationResult;
    }
}
