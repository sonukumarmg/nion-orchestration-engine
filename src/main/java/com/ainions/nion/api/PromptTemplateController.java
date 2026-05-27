package com.ainions.nion.api;

import com.ainions.nion.api.dto.PromptTemplateRequest;
import com.ainions.nion.domain.PromptTemplate;
import com.ainions.nion.repository.PromptTemplateRepository;
import com.ainions.nion.tenant.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptTemplateController {

    private final PromptTemplateRepository repository;

    public PromptTemplateController(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<List<PromptTemplate>> list() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromptTemplate> create(@Valid @RequestBody PromptTemplateRequest request) {
        PromptTemplate template = new PromptTemplate();
        template.setName(request.name());
        template.setVersion(request.version());
        template.setContent(request.content());
        template.setJsonSchema(request.jsonSchema());
        template.setActive(request.active());
        String tenantId = TenantContext.getTenantId();
        template.setTenantId(tenantId == null || tenantId.isBlank() ? "default" : tenantId);
        return ResponseEntity.ok(repository.save(template));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromptTemplate> update(@PathVariable UUID id, @Valid @RequestBody PromptTemplateRequest request) {
        return repository.findById(id)
                .map(template -> {
                    template.setName(request.name());
                    template.setVersion(request.version());
                    template.setContent(request.content());
                    template.setJsonSchema(request.jsonSchema());
                    template.setActive(request.active());
                    return ResponseEntity.ok(repository.save(template));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
