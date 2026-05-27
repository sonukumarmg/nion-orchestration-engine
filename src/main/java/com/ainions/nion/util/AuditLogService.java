package com.ainions.nion.util;

import com.ainions.nion.domain.AuditEvent;
import com.ainions.nion.repository.AuditEventRepository;
import com.ainions.nion.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditLogService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void logEvent(String actor, String action, String target, String metadata) {
        AuditEvent event = new AuditEvent();
        event.setActor(actor);
        event.setAction(action);
        event.setTarget(target);
        event.setMetadata(metadata);
        String tenantId = TenantContext.getTenantId();
        event.setTenantId(tenantId == null || tenantId.isBlank() ? "default" : tenantId);
        auditEventRepository.save(event);
        log.info("audit event actor={} action={} target={} tenant={} metadata={}",
                actor, action, target, TenantContext.getTenantId(), metadata);
    }
}
