package com.company.keyvault.repository;
import com.company.keyvault.model.RefreshSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface RefreshSessionRepository extends MongoRepository<RefreshSession,String> {
 Optional<RefreshSession> findByTokenHash(String tokenHash);
 List<RefreshSession> findByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(String userId);
}
