package com.company.keyvault.repository;

import com.company.keyvault.model.LicenseKey;
import com.company.keyvault.model.enums.KeyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseKeyRepository extends MongoRepository<LicenseKey, String> {

    Optional<LicenseKey> findByKey(String key);

    Optional<LicenseKey> findByKeyAndProductId(String key, String productId);

    Page<LicenseKey> findByProductId(String productId, Pageable pageable);

    Page<LicenseKey> findByStatus(KeyStatus status, Pageable pageable);

    Page<LicenseKey> findByProductIdAndStatus(String productId, KeyStatus status, Pageable pageable);

    List<LicenseKey> findByStatusAndValidUntilBefore(KeyStatus status, Instant now);

    long countByStatus(KeyStatus status);

    long countByProductId(String productId);

    long countByProductIdAndStatus(String productId, KeyStatus status);
}
