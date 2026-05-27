package com.ainions.nion.llm;

import com.ainions.nion.domain.PromptTemplate;
import com.ainions.nion.repository.PromptTemplateRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    private final PromptTemplateRepository promptTemplateRepository;

    public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    public PromptTemplate loadTemplate(String name, String fallbackPath, String schemaPath) {
        return promptTemplateRepository.findFirstByNameAndActiveTrueOrderByVersionDesc(name)
                .orElseGet(() -> fallbackTemplate(name, fallbackPath, schemaPath));
    }

    private PromptTemplate fallbackTemplate(String name, String path, String schemaPath) {
        PromptTemplate template = new PromptTemplate();
        template.setName(name);
        template.setVersion("v1");
        template.setActive(true);
        template.setContent(readResource(path));
        template.setJsonSchema(readResource(schemaPath));
        return template;
    }

    private String readResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }
}
