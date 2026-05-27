package com.ainions.nion.domain;

import com.ainions.nion.domain.enums.MessageType;
import com.ainions.nion.domain.enums.RunStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orchestration_runs")
public class OrchestrationRun extends BaseEntity {

    @Column(nullable = false)
    private String rawMessageId;

    @Column(nullable = false)
    private String messageSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    private String intent;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(length = 4000)
    private String summary;

    @Column(length = 10000)
    private String mapOutput;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlannedTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgentRun> agentRuns = new ArrayList<>();

    public String getRawMessageId() {
        return rawMessageId;
    }

    public void setRawMessageId(String rawMessageId) {
        this.rawMessageId = rawMessageId;
    }

    public String getMessageSource() {
        return messageSource;
    }

    public void setMessageSource(String messageSource) {
        this.messageSource = messageSource;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getMapOutput() {
        return mapOutput;
    }

    public void setMapOutput(String mapOutput) {
        this.mapOutput = mapOutput;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<PlannedTask> getTasks() {
        return tasks;
    }

    public List<AgentRun> getAgentRuns() {
        return agentRuns;
    }
}
