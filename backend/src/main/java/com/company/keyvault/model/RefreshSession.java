package com.company.keyvault.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document(collection="refreshSessions")
public class RefreshSession {
 @Id private String id; @Indexed private String userId; @Indexed(unique=true) private String tokenHash;
 private String ipAddress; private String userAgent; @Builder.Default private Instant createdAt=Instant.now();
 private Instant lastUsedAt; @Indexed(expireAfter="0s") private Instant expiresAt; private Instant revokedAt;
}
