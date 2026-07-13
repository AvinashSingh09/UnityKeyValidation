package com.company.keyvault.controller;

import com.company.keyvault.dto.request.ValidationRequest;
import com.company.keyvault.dto.response.ValidationResponse;
import com.company.keyvault.service.ValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/validate")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/activate")
    public ResponseEntity<ValidationResponse> activate(
            @Valid @RequestBody ValidationRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        ValidationResponse response = validationService.activate(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check")
    public ResponseEntity<ValidationResponse> check(
            @Valid @RequestBody ValidationRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        ValidationResponse response = validationService.check(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<ValidationResponse> deactivate(
            @Valid @RequestBody ValidationRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        ValidationResponse response = validationService.deactivate(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
