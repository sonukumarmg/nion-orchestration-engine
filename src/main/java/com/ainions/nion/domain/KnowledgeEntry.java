package com.ainions.nion.domain;

import com.ainions.nion.domain.enums.KnowledgeStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knowledge_entries")
public class KnowledgeEntry extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 6000)
    private String content;

    @ElementCollection
    @CollectionTable(name = "knowledge_entry_tags", joinColumns = @JoinColumn(name = "knowledge_entry_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KnowledgeStatus status = KnowledgeStatus.ACTIVE;

    private OffsetDateTime lastReviewedAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public KnowledgeStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeStatus status) {
        this.status = status;
    }

    public OffsetDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(OffsetDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }
}
