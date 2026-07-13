package com.company.keyvault.repository;

import com.company.keyvault.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(com.company.keyvault.model.enums.UserRole role);

    long countByRoleAndActiveTrue(com.company.keyvault.model.enums.UserRole role);
}
