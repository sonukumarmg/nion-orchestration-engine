package com.ainions.nion.repository;

import com.ainions.nion.domain.PromptTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {
    Optional<PromptTemplate> findFirstByNameAndActiveTrueOrderByVersionDesc(String name);
}
