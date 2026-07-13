package com.company.keyvault.model;

import com.company.keyvault.model.enums.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "validationLogs")
public class ValidationLog {

    @Id
    private String id;

    private String keyId;

    private String productCode;

    private String hardwareId;

    private String ipAddress;

    private GeoLocation location;

    private String action;  // VALIDATE, ACTIVATE, DEACTIVATE, REJECT

    private ValidationResult result;

    private String reason;  // VALID, KEY_EXPIRED, MAX_ACTIVATIONS, KEY_REVOKED, etc.

    @Builder.Default
    private Instant timestamp = Instant.now();
}
