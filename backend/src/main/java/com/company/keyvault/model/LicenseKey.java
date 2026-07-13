package com.company.keyvault.model;

import com.company.keyvault.model.enums.KeyStatus;
import com.company.keyvault.model.enums.KeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "licenseKeys")
public class LicenseKey {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    @Indexed
    private String productId;

    private String customerName;

    private String customerEmail;

    @Builder.Default
    private KeyStatus status = KeyStatus.ACTIVE;

    @Builder.Default
    private KeyType type = KeyType.TIME_LIMITED;

    @Builder.Default
    private int maxActivations = 1;

    @Builder.Default
    private int currentActivations = 0;

    @Builder.Default
    private List<Activation> activations = new ArrayList<>();

    private Instant validFrom;

    private Instant validUntil;

    private String notes;

    private String createdBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
