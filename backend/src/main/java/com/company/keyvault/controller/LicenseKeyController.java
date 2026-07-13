package com.company.keyvault.controller;

import com.company.keyvault.dto.request.BatchKeyRequest;
import com.company.keyvault.dto.request.DeviceUpdateRequest;
import com.company.keyvault.dto.request.KeyCreateRequest;
import com.company.keyvault.dto.response.KeyResponse;
import com.company.keyvault.dto.response.MessageResponse;
import com.company.keyvault.model.enums.KeyStatus;
import com.company.keyvault.service.LicenseKeyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/keys")
public class LicenseKeyController {

    private final LicenseKeyService keyService;

    public LicenseKeyController(LicenseKeyService keyService) {
        this.keyService = keyService;
    }

    @GetMapping
    public ResponseEntity<Page<KeyResponse>> getAllKeys(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) KeyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<KeyResponse> keys = keyService.getAllKeys(
                productId, status, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KeyResponse> getKey(@PathVariable String id) {
        return ResponseEntity.ok(keyService.getKeyById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KeyResponse> createKey(
            @Valid @RequestBody KeyCreateRequest request,
            Authentication authentication) {
        KeyResponse key = keyService.createKey(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(key);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<KeyResponse>> batchCreateKeys(
            @Valid @RequestBody BatchKeyRequest request,
            Authentication authentication) {
        List<KeyResponse> keys = keyService.batchCreateKeys(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(keys);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KeyResponse> updateKey(
            @PathVariable String id,
            @Valid @RequestBody KeyCreateRequest request) {
        return ResponseEntity.ok(keyService.updateKey(id, request));
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KeyResponse> revokeKey(@PathVariable String id) {
        return ResponseEntity.ok(keyService.revokeKey(id));
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KeyResponse> suspendKey(@PathVariable String id) {
        return ResponseEntity.ok(keyService.suspendKey(id));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KeyResponse> reactivateKey(@PathVariable String id) {
        return ResponseEntity.ok(keyService.reactivateKey(id));
    }

    @PutMapping("/{id}/devices/{hardwareId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KeyResponse> updateDevice(
            @PathVariable String id,
            @PathVariable String hardwareId,
            @Valid @RequestBody DeviceUpdateRequest request) {
        return ResponseEntity.ok(keyService.updateDevice(id, hardwareId, request));
    }

    @DeleteMapping("/{id}/devices/{hardwareId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<KeyResponse> removeDevice(
            @PathVariable String id,
            @PathVariable String hardwareId) {
        return ResponseEntity.ok(keyService.removeDevice(id, hardwareId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> deleteKey(@PathVariable String id) {
        keyService.deleteKey(id);
        return ResponseEntity.ok(MessageResponse.of("Key deleted successfully"));
    }
}
