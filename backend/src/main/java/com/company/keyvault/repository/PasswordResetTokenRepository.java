package com.company.keyvault.repository;
import com.company.keyvault.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken,String> { Optional<PasswordResetToken> findByTokenHash(String tokenHash); List<PasswordResetToken> findByUserIdAndUsedAtIsNull(String userId); }
