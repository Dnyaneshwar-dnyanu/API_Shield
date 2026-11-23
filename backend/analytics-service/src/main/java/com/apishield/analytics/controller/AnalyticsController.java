package com.apishield.analytics.controller;

import com.apishield.analytics.dto.*;
import com.apishield.analytics.service.AnalyticsIngestionService;
import com.apishield.analytics.service.AnalyticsQueryService;
import com.apishield.common.constant.HeaderConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsQueryService queryService;
    private final AnalyticsIngestionService ingestionService;

    @Value("${apishield.security.internal-token}")
    private String configuredInternalToken;

    @GetMapping("/owner/overview")
    public ResponseEntity<OwnerOverviewResponse> getOwnerOverview(
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String ownerId,
            @RequestParam(required = false) String apiId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        log.info("Fetching owner analytics overview: ownerId={}, apiId={}, days={}", ownerId, apiId, days);
        OwnerOverviewResponse overview = queryService.getOwnerOverview(ownerId, apiId, days);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/owner/apis/{apiId}/timeseries")
    public ResponseEntity<List<TimeseriesPointResponse>> getTimeseries(
            @PathVariable String apiId,
            @RequestParam(required = false, defaultValue = "7") Integer days) {
        log.info("Fetching timeseries analytics: apiId={}, days={}", apiId, days);
        List<TimeseriesPointResponse> timeseries = queryService.getTimeseries(apiId, days);
        return ResponseEntity.ok(timeseries);
    }

    @GetMapping("/developer/quota")
    public ResponseEntity<DeveloperQuotaResponse> getDeveloperQuota(
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("Fetching developer quota analytics: developerId={}", developerId);
        DeveloperQuotaResponse quota = queryService.getDeveloperQuota(developerId);
        return ResponseEntity.ok(quota);
    }

    @GetMapping("/developer/logs")
    public ResponseEntity<Page<RequestLogResponse>> getDeveloperLogs(
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId,
            @RequestParam(required = false) String apiId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Fetching developer request logs: developerId={}, apiId={}", developerId, apiId);
        Page<RequestLogResponse> logs = queryService.getDeveloperLogs(developerId, apiId, pageable);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/telemetry/ingest")
    public ResponseEntity<Void> ingestTelemetry(
            @RequestHeader(value = HeaderConstants.X_INTERNAL_TOKEN, required = false) String internalToken,
            @RequestBody GatewayTelemetryMessage message) {
        if (internalToken == null || !internalToken.equals(configuredInternalToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal communication token");
        }
        ingestionService.ingest(message);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
