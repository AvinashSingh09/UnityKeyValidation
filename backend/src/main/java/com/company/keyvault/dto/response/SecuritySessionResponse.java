package com.company.keyvault.dto.response;
import com.company.keyvault.model.RefreshSession; import lombok.*; import java.time.Instant;
@Data @Builder public class SecuritySessionResponse { private String id; private String ipAddress; private String userAgent; private Instant createdAt; private Instant lastUsedAt; private Instant expiresAt;
 public static SecuritySessionResponse from(RefreshSession s){return builder().id(s.getId()).ipAddress(s.getIpAddress()).userAgent(s.getUserAgent()).createdAt(s.getCreatedAt()).lastUsedAt(s.getLastUsedAt()).expiresAt(s.getExpiresAt()).build();}}
