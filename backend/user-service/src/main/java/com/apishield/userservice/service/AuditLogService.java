package com.apishield.userservice.service;

import com.apishield.userservice.dto.response.AuditLogResponse;
import com.apishield.userservice.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogService {

    void logAction(UUID actorId, UUID targetId, String action, String reason);

    PageResponse<AuditLogResponse> listAuditLogs(UUID targetId, Pageable pageable);
}
