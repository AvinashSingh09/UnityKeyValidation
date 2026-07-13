package com.company.keyvault.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activation {

    private String hardwareId;
    private Instant activatedAt;
    private Instant lastValidatedAt;
    private String ipAddress;
    private String machineName;
    @Builder.Default
    private boolean trusted = false;
    private GeoLocation location;
}
