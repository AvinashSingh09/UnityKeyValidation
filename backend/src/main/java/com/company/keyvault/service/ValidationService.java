package com.company.keyvault.service;

import com.company.keyvault.dto.request.ValidationRequest;
import com.company.keyvault.dto.response.ValidationResponse;
import com.company.keyvault.model.Activation;
import com.company.keyvault.model.GeoLocation;
import com.company.keyvault.model.LicenseKey;
import com.company.keyvault.model.Product;
import com.company.keyvault.model.ValidationLog;
import com.company.keyvault.model.enums.KeyStatus;
import com.company.keyvault.model.enums.ValidationResult;
import com.company.keyvault.repository.LicenseKeyRepository;
import com.company.keyvault.repository.ProductRepository;
import com.company.keyvault.repository.ValidationLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class ValidationService {

    private final LicenseKeyRepository keyRepository;
    private final ProductRepository productRepository;
    private final ValidationLogRepository logRepository;
    private final GeoIpService geoIpService;

    public ValidationService(LicenseKeyRepository keyRepository,
                              ProductRepository productRepository,
                              ValidationLogRepository logRepository,
                              GeoIpService geoIpService) {
        this.keyRepository = keyRepository;
        this.productRepository = productRepository;
        this.logRepository = logRepository;
        this.geoIpService = geoIpService;
    }

    /**
     * Activate a key on a new machine. If already activated on this hardware, treat as a check.
     */
    public ValidationResponse activate(ValidationRequest request, String ipAddress) {
        // Find product by code
        Optional<Product> productOpt = productRepository.findByProductCode(request.getProductCode());
        if (productOpt.isEmpty()) {
            logValidation(null, request, ipAddress, "ACTIVATE", ValidationResult.FAILED, "PRODUCT_NOT_FOUND");
            return buildResponse(false, "PRODUCT_NOT_FOUND", null, null, request.getProductCode());
        }
        Product product = productOpt.get();

        // Find key
        Optional<LicenseKey> keyOpt = keyRepository.findByKeyAndProductId(
                request.getKey(), product.getId());
        if (keyOpt.isEmpty()) {
            logValidation(null, request, ipAddress, "ACTIVATE", ValidationResult.FAILED, "INVALID_KEY");
            return buildResponse(false, "INVALID_KEY", null, null, request.getProductCode());
        }
        LicenseKey key = keyOpt.get();

        // Check key status
        if (key.getStatus() != KeyStatus.ACTIVE) {
            logValidation(key.getId(), request, ipAddress, "ACTIVATE", ValidationResult.FAILED,
                    "KEY_" + key.getStatus().name());
            return buildResponse(false, "KEY_" + key.getStatus().name(), key.getStatus(),
                    key.getValidUntil(), request.getProductCode());
        }

        // Check expiry
        if (key.getValidUntil() != null && key.getValidUntil().isBefore(Instant.now())) {
            key.setStatus(KeyStatus.EXPIRED);
            keyRepository.save(key);
            logValidation(key.getId(), request, ipAddress, "ACTIVATE", ValidationResult.FAILED, "KEY_EXPIRED");
            return buildResponse(false, "KEY_EXPIRED", KeyStatus.EXPIRED,
                    key.getValidUntil(), request.getProductCode());
        }

        // Check if already activated on this hardware
        Optional<Activation> existingActivation = key.getActivations().stream()
                .filter(a -> a.getHardwareId().equals(request.getHardwareId()))
                .findFirst();

        if (existingActivation.isPresent()) {
            // Already activated — just update last validated
            GeoLocation location = updateLastSeen(existingActivation.get(), ipAddress);
            keyRepository.save(key);
            logValidation(key.getId(), request, ipAddress, location,
                    "ACTIVATE", ValidationResult.SUCCESS, "ALREADY_ACTIVATED");
            return buildResponse(true, "VALID", key.getStatus(),
                    key.getValidUntil(), request.getProductCode());
        }

        // Check max activations
        if (key.getCurrentActivations() >= key.getMaxActivations()) {
            logValidation(key.getId(), request, ipAddress, "ACTIVATE", ValidationResult.FAILED, "MAX_ACTIVATIONS_REACHED");
            return buildResponse(false, "MAX_ACTIVATIONS_REACHED", key.getStatus(),
                    key.getValidUntil(), request.getProductCode());
        }

        // Activate on this hardware
        GeoLocation location = geoIpService.lookup(ipAddress).orElse(null);
        Activation activation = Activation.builder()
                .hardwareId(request.getHardwareId())
                .activatedAt(Instant.now())
                .lastValidatedAt(Instant.now())
                .ipAddress(ipAddress)
                .machineName(request.getMachineName())
                .location(location)
                .build();

        key.getActivations().add(activation);
        key.setCurrentActivations(key.getCurrentActivations() + 1);
        keyRepository.save(key);

        logValidation(key.getId(), request, ipAddress, location,
                "ACTIVATE", ValidationResult.SUCCESS, "ACTIVATED");
        return buildResponse(true, "VALID", key.getStatus(),
                key.getValidUntil(), request.getProductCode());
    }

    /**
     * Periodic heartbeat check — updates lastValidatedAt.
     */
    public ValidationResponse check(ValidationRequest request, String ipAddress) {
        Optional<Product> productOpt = productRepository.findByProductCode(request.getProductCode());
        if (productOpt.isEmpty()) {
            logValidation(null, request, ipAddress, "VALIDATE", ValidationResult.FAILED, "PRODUCT_NOT_FOUND");
            return buildResponse(false, "PRODUCT_NOT_FOUND", null, null, request.getProductCode());
        }
        Product product = productOpt.get();

        Optional<LicenseKey> keyOpt = keyRepository.findByKeyAndProductId(
                request.getKey(), product.getId());
        if (keyOpt.isEmpty()) {
            logValidation(null, request, ipAddress, "VALIDATE", ValidationResult.FAILED, "INVALID_KEY");
            return buildResponse(false, "INVALID_KEY", null, null, request.getProductCode());
        }
        LicenseKey key = keyOpt.get();

        // Check status
        if (key.getStatus() != KeyStatus.ACTIVE) {
            logValidation(key.getId(), request, ipAddress, "VALIDATE", ValidationResult.FAILED,
                    "KEY_" + key.getStatus().name());
            return buildResponse(false, "KEY_" + key.getStatus().name(), key.getStatus(),
                    key.getValidUntil(), request.getProductCode());
        }

        // Check expiry
        if (key.getValidUntil() != null && key.getValidUntil().isBefore(Instant.now())) {
            key.setStatus(KeyStatus.EXPIRED);
            keyRepository.save(key);
            logValidation(key.getId(), request, ipAddress, "VALIDATE", ValidationResult.FAILED, "KEY_EXPIRED");
            return buildResponse(false, "KEY_EXPIRED", KeyStatus.EXPIRED,
                    key.getValidUntil(), request.getProductCode());
        }

        // Check if activated on this hardware
        Optional<Activation> activation = key.getActivations().stream()
                .filter(a -> a.getHardwareId().equals(request.getHardwareId()))
                .findFirst();

        if (activation.isEmpty()) {
            logValidation(key.getId(), request, ipAddress, "VALIDATE", ValidationResult.FAILED, "NOT_ACTIVATED");
            return buildResponse(false, "NOT_ACTIVATED", key.getStatus(),
                    key.getValidUntil(), request.getProductCode());
        }

        // Update last validated
        GeoLocation location = updateLastSeen(activation.get(), ipAddress);
        keyRepository.save(key);

        logValidation(key.getId(), request, ipAddress, location,
                "VALIDATE", ValidationResult.SUCCESS, "VALID");
        return buildResponse(true, "VALID", key.getStatus(),
                key.getValidUntil(), request.getProductCode());
    }

    /**
     * Deactivate a key from a specific machine.
     */
    public ValidationResponse deactivate(ValidationRequest request, String ipAddress) {
        Optional<Product> productOpt = productRepository.findByProductCode(request.getProductCode());
        if (productOpt.isEmpty()) {
            logValidation(null, request, ipAddress, "DEACTIVATE", ValidationResult.FAILED, "PRODUCT_NOT_FOUND");
            return buildResponse(false, "PRODUCT_NOT_FOUND", null, null, request.getProductCode());
        }
        Product product = productOpt.get();

        Optional<LicenseKey> keyOpt = keyRepository.findByKeyAndProductId(
                request.getKey(), product.getId());
        if (keyOpt.isEmpty()) {
            logValidation(null, request, ipAddress, "DEACTIVATE", ValidationResult.FAILED, "INVALID_KEY");
            return buildResponse(false, "INVALID_KEY", null, null, request.getProductCode());
        }
        LicenseKey key = keyOpt.get();

        boolean removed = key.getActivations().removeIf(
                a -> a.getHardwareId().equals(request.getHardwareId()));

        if (removed) {
            key.setCurrentActivations(Math.max(0, key.getCurrentActivations() - 1));
            keyRepository.save(key);
            logValidation(key.getId(), request, ipAddress, "DEACTIVATE", ValidationResult.SUCCESS, "DEACTIVATED");
            return buildResponse(true, "DEACTIVATED", key.getStatus(),
                    key.getValidUntil(), request.getProductCode());
        } else {
            logValidation(key.getId(), request, ipAddress, "DEACTIVATE", ValidationResult.FAILED, "NOT_ACTIVATED");
            return buildResponse(false, "NOT_ACTIVATED", key.getStatus(),
                    key.getValidUntil(), request.getProductCode());
        }
    }

    private ValidationResponse buildResponse(boolean valid, String reason, KeyStatus status,
                                               Instant validUntil, String productCode) {
        return ValidationResponse.builder()
                .valid(valid)
                .reason(reason)
                .status(status)
                .validUntil(validUntil)
                .productCode(productCode)
                .build();
    }

    private void logValidation(String keyId, ValidationRequest request, String ipAddress,
                                String action, ValidationResult result, String reason) {
        logValidation(keyId, request, ipAddress, null, action, result, reason);
    }

    private void logValidation(String keyId, ValidationRequest request, String ipAddress,
                               GeoLocation location, String action,
                               ValidationResult result, String reason) {
        ValidationLog log = ValidationLog.builder()
                .keyId(keyId)
                .productCode(request.getProductCode())
                .hardwareId(request.getHardwareId())
                .ipAddress(ipAddress)
                .location(location)
                .action(action)
                .result(result)
                .reason(reason)
                .timestamp(Instant.now())
                .build();
        logRepository.save(log);
    }

    private GeoLocation updateLastSeen(Activation activation, String ipAddress) {
        boolean ipChanged = activation.getIpAddress() == null
                || !activation.getIpAddress().equals(ipAddress);
        activation.setLastValidatedAt(Instant.now());
        activation.setIpAddress(ipAddress);
        if (ipChanged || activation.getLocation() == null) {
            geoIpService.lookup(ipAddress).ifPresent(activation::setLocation);
        }
        return activation.getLocation();
    }
}
