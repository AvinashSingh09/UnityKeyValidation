package com.company.keyvault.repository;

import com.company.keyvault.model.ValidationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ValidationLogRepository extends MongoRepository<ValidationLog, String> {

    Page<ValidationLog> findByKeyIdOrderByTimestampDesc(String keyId, Pageable pageable);

    Page<ValidationLog> findAllByOrderByTimestampDesc(Pageable pageable);

    List<ValidationLog> findByTimestampBetween(Instant start, Instant end);

    long countByResultAndTimestampBetween(com.company.keyvault.model.enums.ValidationResult result, Instant start, Instant end);
}
