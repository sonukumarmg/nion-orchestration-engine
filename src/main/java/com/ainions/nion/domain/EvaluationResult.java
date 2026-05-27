package com.ainions.nion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "evaluation_results")
public class EvaluationResult extends BaseEntity {

    private boolean toneOk;

    private int accuracyScore;

    private int completenessScore;

    @Column(length = 2000)
    private String flags;

    private String overallQuality;

    @Column(length = 2000)
    private String recommendation;

    @Column(length = 6000)
    private String rawOutput;

    @OneToOne
    @JoinColumn(name = "run_id")
    private OrchestrationRun run;

    public boolean isToneOk() {
        return toneOk;
    }

    public void setToneOk(boolean toneOk) {
        this.toneOk = toneOk;
    }

    public int getAccuracyScore() {
        return accuracyScore;
    }

    public void setAccuracyScore(int accuracyScore) {
        this.accuracyScore = accuracyScore;
    }

    public int getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(int completenessScore) {
        this.completenessScore = completenessScore;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public String getOverallQuality() {
        return overallQuality;
    }

    public void setOverallQuality(String overallQuality) {
        this.overallQuality = overallQuality;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getRawOutput() {
        return rawOutput;
    }

    public void setRawOutput(String rawOutput) {
        this.rawOutput = rawOutput;
    }

    public OrchestrationRun getRun() {
        return run;
    }

    public void setRun(OrchestrationRun run) {
        this.run = run;
    }
}
