package com.librarymanagement.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, String actionType, String entityType, String entityId, String correlationId, String details) {
        try {
            AuditEvent event = new AuditEvent(actor, actionType, entityType, entityId, correlationId, "127.0.0.1", details);
            auditEventRepository.save(event);
            log.info("AUDIT: actor='{}' action='{}' entity='{}:{}' corr='{}'", actor, actionType, entityType, entityId, correlationId);
        } catch (Exception ex) {
            log.error("Failed to persist audit event for action {}: {}", actionType, ex.getMessage());
        }
    }
}
