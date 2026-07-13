package com.company.keyvault.dto.response;

import com.company.keyvault.model.Activation;
import com.company.keyvault.model.LicenseKey;
import com.company.keyvault.model.enums.KeyStatus;
import com.company.keyvault.model.enums.KeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyResponse {

    private String id;
    private String key;
    private String productId;
    private String productName;
    private String customerName;
    private String customerEmail;
    private KeyStatus status;
    private KeyType type;
    private int maxActivations;
    private int currentActivations;
    private List<Activation> activations;
    private Instant validFrom;
    private Instant validUntil;
    private String notes;
    private Instant createdAt;

    public static KeyResponse from(LicenseKey licenseKey, String productName) {
        return KeyResponse.builder()
                .id(licenseKey.getId())
                .key(licenseKey.getKey())
                .productId(licenseKey.getProductId())
                .productName(productName)
                .customerName(licenseKey.getCustomerName())
                .customerEmail(licenseKey.getCustomerEmail())
                .status(licenseKey.getStatus())
                .type(licenseKey.getType())
                .maxActivations(licenseKey.getMaxActivations())
                .currentActivations(licenseKey.getCurrentActivations())
                .activations(licenseKey.getActivations())
                .validFrom(licenseKey.getValidFrom())
                .validUntil(licenseKey.getValidUntil())
                .notes(licenseKey.getNotes())
                .createdAt(licenseKey.getCreatedAt())
                .build();
    }
}
