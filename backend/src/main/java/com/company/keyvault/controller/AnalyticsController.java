package com.company.keyvault.controller;

import com.company.keyvault.dto.response.DashboardResponse;
import com.company.keyvault.dto.response.AnalyticsInsightsResponse;
import com.company.keyvault.dto.response.GeographyResponse;
import com.company.keyvault.model.ValidationLog;
import com.company.keyvault.repository.ValidationLogRepository;
import com.company.keyvault.service.AnalyticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ValidationLogRepository logRepository;

    public AnalyticsController(AnalyticsService analyticsService,
                                ValidationLogRepository logRepository) {
        this.analyticsService = analyticsService;
        this.logRepository = logRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }

    @GetMapping("/geography")
    public ResponseEntity<GeographyResponse> getGeography() {
        return ResponseEntity.ok(analyticsService.getGeography());
    }

    @GetMapping("/insights")
    public ResponseEntity<AnalyticsInsightsResponse> getInsights(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(analyticsService.getInsights(days));
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<ValidationLog>> getLogs(
            @RequestParam(required = false) String keyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<ValidationLog> logs = keyId == null || keyId.isBlank()
                ? logRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size))
                : logRepository.findByKeyIdOrderByTimestampDesc(keyId, PageRequest.of(page, size));
        return ResponseEntity.ok(logs);
    }
}
