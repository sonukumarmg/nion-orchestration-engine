package com.ainions.nion.orchestration;

import com.ainions.nion.domain.KnowledgeEntry;
import com.ainions.nion.repository.KnowledgeEntryRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {

    private final KnowledgeEntryRepository knowledgeEntryRepository;

    public KnowledgeRetrievalService(KnowledgeEntryRepository knowledgeEntryRepository) {
        this.knowledgeEntryRepository = knowledgeEntryRepository;
    }

    @Cacheable("knowledge-context")
    public String retrieveContext(String message) {
        List<KnowledgeEntry> entries = knowledgeEntryRepository.findAll();
        if (entries.isEmpty()) {
            return "Project Phoenix baseline context: Sprint 14 active, API integration at 70%, July 31 deadline.";
        }
        return entries.stream()
                .limit(5)
                .map(entry -> entry.getTitle() + ": " + entry.getContent())
                .collect(Collectors.joining("\n"));
    }
}
