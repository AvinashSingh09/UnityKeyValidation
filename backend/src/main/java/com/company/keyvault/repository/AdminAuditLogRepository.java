package com.company.keyvault.repository;
import com.company.keyvault.model.AdminAuditLog;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface AdminAuditLogRepository extends MongoRepository<AdminAuditLog,String> { Page<AdminAuditLog> findAllByOrderByTimestampDesc(Pageable pageable); }
