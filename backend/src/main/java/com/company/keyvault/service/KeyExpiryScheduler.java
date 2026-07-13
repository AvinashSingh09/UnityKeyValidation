package com.company.keyvault.service;

import com.company.keyvault.model.LicenseKey;
import com.company.keyvault.model.enums.KeyStatus;
import com.company.keyvault.repository.LicenseKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class KeyExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeyExpiryScheduler.class);

    private final LicenseKeyRepository keyRepository;

    public KeyExpiryScheduler(LicenseKeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    /**
     * Runs every hour to mark expired keys.
     */
    @Scheduled(cron = "${app.key-expiry-check-cron}")
    public void expireKeys() {
        List<LicenseKey> expiredKeys = keyRepository
                .findByStatusAndValidUntilBefore(KeyStatus.ACTIVE, Instant.now());

        if (!expiredKeys.isEmpty()) {
            log.info("Found {} keys to expire", expiredKeys.size());
            for (LicenseKey key : expiredKeys) {
                key.setStatus(KeyStatus.EXPIRED);
                keyRepository.save(key);
                log.info("Expired key: {} (product: {})", key.getKey(), key.getProductId());
            }
        }
    }
}
