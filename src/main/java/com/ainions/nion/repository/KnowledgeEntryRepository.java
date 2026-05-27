package com.ainions.nion.repository;

import com.ainions.nion.domain.KnowledgeEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, UUID> {
}
