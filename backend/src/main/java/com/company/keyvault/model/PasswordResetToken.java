package com.company.keyvault.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document(collection="passwordResetTokens")
public class PasswordResetToken {
 @Id private String id; @Indexed private String userId; @Indexed(unique=true) private String tokenHash;
 @Builder.Default private Instant createdAt=Instant.now(); @Indexed(expireAfter="0s") private Instant expiresAt; private Instant usedAt;
}
