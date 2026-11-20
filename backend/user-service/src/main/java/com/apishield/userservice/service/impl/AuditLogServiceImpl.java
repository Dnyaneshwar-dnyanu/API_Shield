package com.apishield.userservice.service.impl;

import com.apishield.userservice.dto.response.AuditLogResponse;
import com.apishield.userservice.dto.response.PageResponse;
import com.apishield.userservice.entity.AuditLog;
import com.apishield.userservice.repository.AuditLogRepository;
import com.apishield.userservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logAction(UUID actorId, UUID targetId, String action, String reason) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .targetId(targetId)
                .action(action)
                .reason(reason)
                .build();

        auditLogRepository.save(auditLog);
        log.info("Persisted audit log: actor={}, target={}, action={}, reason={}", actorId, targetId, action, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(UUID targetId, Pageable pageable) {
        Page<AuditLog> page = (targetId != null)
                ? auditLogRepository.findByTargetId(targetId, pageable)
                : auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageResponse.of(page.map(AuditLogResponse::fromEntity));
    }
}
