package com.company.keyvault.dto.response;
import com.company.keyvault.model.User; import com.company.keyvault.model.enums.UserRole; import lombok.*; import java.time.Instant;
@Data @Builder public class UserResponse { private String id; private String email; private String fullName; private UserRole role; private boolean active; private Instant lastLoginAt; private Instant createdAt;
 public static UserResponse from(User u){return builder().id(u.getId()).email(u.getEmail()).fullName(u.getFullName()).role(u.getRole()).active(u.isActive()).lastLoginAt(u.getLastLoginAt()).createdAt(u.getCreatedAt()).build();}}
