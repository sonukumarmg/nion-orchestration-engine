package com.ainions.nion.api;

import com.ainions.nion.api.dto.KnowledgeEntryRequest;
import com.ainions.nion.domain.KnowledgeEntry;
import com.ainions.nion.repository.KnowledgeEntryRepository;
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
@RequestMapping("/api/v1/knowledge")
public class KnowledgeBaseController {

    private final KnowledgeEntryRepository repository;

    public KnowledgeBaseController(KnowledgeEntryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','VIEWER')")
    public ResponseEntity<List<KnowledgeEntry>> list() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<KnowledgeEntry> create(@Valid @RequestBody KnowledgeEntryRequest request) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(request.title());
        entry.setContent(request.content());
        entry.setTags(request.tags());
        String tenantId = TenantContext.getTenantId();
        entry.setTenantId(tenantId == null || tenantId.isBlank() ? "default" : tenantId);
        return ResponseEntity.ok(repository.save(entry));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<KnowledgeEntry> update(@PathVariable UUID id, @Valid @RequestBody KnowledgeEntryRequest request) {
        return repository.findById(id)
                .map(entry -> {
                    entry.setTitle(request.title());
                    entry.setContent(request.content());
                    entry.setTags(request.tags());
                    return ResponseEntity.ok(repository.save(entry));
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
