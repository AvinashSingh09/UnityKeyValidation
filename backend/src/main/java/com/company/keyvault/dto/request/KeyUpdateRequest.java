package com.company.keyvault.dto.request;

import com.company.keyvault.model.enums.KeyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.Instant;

@Data
public class KeyUpdateRequest {
    @Size(max = 160) private String customerName;
    @Email private String customerEmail;
    @NotNull private KeyType type;
    @NotNull @Min(value = 1, message = "Maximum devices must be at least 1") private Integer maxActivations;
    @NotNull private Instant validFrom;
    private Instant validUntil;
    @Size(max = 1000) private String notes;
}
