package com.company.keyvault.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class KeyGeneratorService {

    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SEGMENT_LENGTH = 5;
    private static final int SEGMENT_COUNT = 4;
    private static final String SEPARATOR = "-";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a cryptographically secure license key in XXXXX-XXXXX-XXXXX-XXXXX format.
     */
    public String generateKey() {
        StringBuilder key = new StringBuilder();

        for (int seg = 0; seg < SEGMENT_COUNT; seg++) {
            if (seg > 0) {
                key.append(SEPARATOR);
            }
            for (int ch = 0; ch < SEGMENT_LENGTH; ch++) {
                int index = secureRandom.nextInt(CHARSET.length());
                key.append(CHARSET.charAt(index));
            }
        }

        return key.toString();
    }
}
