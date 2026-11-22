package com.librarymanagement.common.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditEvent() {}

    public AuditEvent(String actor, String actionType, String entityType, String entityId, String correlationId, String ipAddress, String details) {
        this.actor = actor;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.correlationId = correlationId;
        this.ipAddress = ipAddress;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getActor() { return actor; }
    public String getActionType() { return actionType; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getCorrelationId() { return correlationId; }
    public String getIpAddress() { return ipAddress; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
