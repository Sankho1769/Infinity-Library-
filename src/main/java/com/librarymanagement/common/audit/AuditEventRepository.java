package com.librarymanagement.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findByActor(String actor, Pageable pageable);
    Page<AuditEvent> findByActionType(String actionType, Pageable pageable);
}
