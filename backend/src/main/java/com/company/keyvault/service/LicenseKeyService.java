package com.company.keyvault.service;

import com.company.keyvault.dto.request.BatchKeyRequest;
import com.company.keyvault.dto.request.DeviceUpdateRequest;
import com.company.keyvault.dto.request.KeyCreateRequest;
import com.company.keyvault.dto.response.KeyResponse;
import com.company.keyvault.exception.ResourceNotFoundException;
import com.company.keyvault.model.Activation;
import com.company.keyvault.model.LicenseKey;
import com.company.keyvault.model.Product;
import com.company.keyvault.model.enums.KeyStatus;
import com.company.keyvault.repository.LicenseKeyRepository;
import com.company.keyvault.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class LicenseKeyService {

    private final LicenseKeyRepository keyRepository;
    private final ProductRepository productRepository;
    private final KeyGeneratorService keyGeneratorService;

    public LicenseKeyService(LicenseKeyRepository keyRepository,
                              ProductRepository productRepository,
                              KeyGeneratorService keyGeneratorService) {
        this.keyRepository = keyRepository;
        this.productRepository = productRepository;
        this.keyGeneratorService = keyGeneratorService;
    }

    public Page<KeyResponse> getAllKeys(String productId, KeyStatus status, Pageable pageable) {
        Page<LicenseKey> keys;

        if (productId != null && status != null) {
            keys = keyRepository.findByProductIdAndStatus(productId, status, pageable);
        } else if (productId != null) {
            keys = keyRepository.findByProductId(productId, pageable);
        } else if (status != null) {
            keys = keyRepository.findByStatus(status, pageable);
        } else {
            keys = keyRepository.findAll(pageable);
        }

        return keys.map(key -> {
            String productName = productRepository.findById(key.getProductId())
                    .map(Product::getName).orElse("Unknown");
            return KeyResponse.from(key, productName);
        });
    }

    public KeyResponse getKeyById(String id) {
        LicenseKey key = keyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License key not found with id: " + id));
        String productName = productRepository.findById(key.getProductId())
                .map(Product::getName).orElse("Unknown");
        return KeyResponse.from(key, productName);
    }

    public KeyResponse createKey(KeyCreateRequest request, String createdBy) {
        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        String generatedKey = keyGeneratorService.generateKey();

        LicenseKey licenseKey = LicenseKey.builder()
                .key(generatedKey)
                .productId(request.getProductId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .status(KeyStatus.ACTIVE)
                .type(request.getType())
                .maxActivations(request.getMaxActivations())
                .currentActivations(0)
                .activations(new ArrayList<>())
                .validFrom(request.getValidFrom() != null ? request.getValidFrom() : Instant.now())
                .validUntil(request.getValidUntil())
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        LicenseKey saved = keyRepository.save(licenseKey);
        return KeyResponse.from(saved, product.getName());
    }

    public List<KeyResponse> batchCreateKeys(BatchKeyRequest request, String createdBy) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        List<KeyResponse> results = new ArrayList<>();

        for (int i = 0; i < request.getCount(); i++) {
            String generatedKey = keyGeneratorService.generateKey();

            LicenseKey licenseKey = LicenseKey.builder()
                    .key(generatedKey)
                    .productId(request.getProductId())
                    .customerName(request.getCustomerName())
                    .status(KeyStatus.ACTIVE)
                    .type(request.getType())
                    .maxActivations(request.getMaxActivations())
                    .currentActivations(0)
                    .activations(new ArrayList<>())
                    .validFrom(request.getValidFrom() != null ? request.getValidFrom() : Instant.now())
                    .validUntil(request.getValidUntil())
                    .notes(request.getNotes())
                    .createdBy(createdBy)
                    .build();

            LicenseKey saved = keyRepository.save(licenseKey);
            results.add(KeyResponse.from(saved, product.getName()));
        }

        return results;
    }

    public KeyResponse updateKey(String id, KeyCreateRequest request) {
        LicenseKey key = keyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License key not found with id: " + id));

        if (request.getCustomerName() != null) key.setCustomerName(request.getCustomerName());
        if (request.getCustomerEmail() != null) key.setCustomerEmail(request.getCustomerEmail());
        if (request.getType() != null) key.setType(request.getType());
        if (request.getMaxActivations() > 0) key.setMaxActivations(request.getMaxActivations());
        if (request.getValidFrom() != null) key.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) key.setValidUntil(request.getValidUntil());
        if (request.getNotes() != null) key.setNotes(request.getNotes());

        LicenseKey saved = keyRepository.save(key);
        String productName = productRepository.findById(saved.getProductId())
                .map(Product::getName).orElse("Unknown");
        return KeyResponse.from(saved, productName);
    }

    public KeyResponse revokeKey(String id) {
        return updateKeyStatus(id, KeyStatus.REVOKED);
    }

    public KeyResponse suspendKey(String id) {
        return updateKeyStatus(id, KeyStatus.SUSPENDED);
    }

    public KeyResponse reactivateKey(String id) {
        return updateKeyStatus(id, KeyStatus.ACTIVE);
    }

    public void deleteKey(String id) {
        if (!keyRepository.existsById(id)) {
            throw new ResourceNotFoundException("License key not found with id: " + id);
        }
        keyRepository.deleteById(id);
    }

    public KeyResponse updateDevice(String id, String hardwareId, DeviceUpdateRequest request) {
        LicenseKey key = getLicenseKey(id);
        Activation activation = findActivation(key, hardwareId);

        if (request.getMachineName() != null) {
            String machineName = request.getMachineName().trim();
            activation.setMachineName(machineName.isEmpty() ? null : machineName);
        }
        if (request.getTrusted() != null) {
            activation.setTrusted(request.getTrusted());
        }

        return toResponse(keyRepository.save(key));
    }

    public KeyResponse removeDevice(String id, String hardwareId) {
        LicenseKey key = getLicenseKey(id);
        findActivation(key, hardwareId);
        key.getActivations().removeIf(activation -> hardwareId.equals(activation.getHardwareId()));
        key.setCurrentActivations(key.getActivations().size());
        return toResponse(keyRepository.save(key));
    }

    private KeyResponse updateKeyStatus(String id, KeyStatus status) {
        LicenseKey key = keyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License key not found with id: " + id));
        key.setStatus(status);
        LicenseKey saved = keyRepository.save(key);
        String productName = productRepository.findById(saved.getProductId())
                .map(Product::getName).orElse("Unknown");
        return KeyResponse.from(saved, productName);
    }

    private LicenseKey getLicenseKey(String id) {
        LicenseKey key = keyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License key not found with id: " + id));
        if (key.getActivations() == null) {
            key.setActivations(new ArrayList<>());
        }
        return key;
    }

    private Activation findActivation(LicenseKey key, String hardwareId) {
        return key.getActivations().stream()
                .filter(activation -> hardwareId.equals(activation.getHardwareId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not found for this license key"));
    }

    private KeyResponse toResponse(LicenseKey key) {
        String productName = productRepository.findById(key.getProductId())
                .map(Product::getName).orElse("Unknown");
        return KeyResponse.from(key, productName);
    }
}
