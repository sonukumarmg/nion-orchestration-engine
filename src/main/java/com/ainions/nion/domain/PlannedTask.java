package com.ainions.nion.domain;

import com.ainions.nion.domain.enums.L2Domain;
import com.ainions.nion.domain.enums.PriorityLevel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planned_tasks")
public class PlannedTask extends BaseEntity {

    @Column(nullable = false)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private L2Domain domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityLevel priority;

    @Column(length = 2000)
    private String rationale;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "planned_task_agent_hints", joinColumns = @JoinColumn(name = "planned_task_id"))
    @Column(name = "agent_hint")
    private List<String> agentHints = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "run_id")
    private OrchestrationRun run;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public L2Domain getDomain() {
        return domain;
    }

    public void setDomain(L2Domain domain) {
        this.domain = domain;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public List<String> getAgentHints() {
        return agentHints;
    }

    public void setAgentHints(List<String> agentHints) {
        this.agentHints = agentHints;
    }

    public OrchestrationRun getRun() {
        return run;
    }

    public void setRun(OrchestrationRun run) {
        this.run = run;
    }
}
