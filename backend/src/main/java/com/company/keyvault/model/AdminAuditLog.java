package com.company.keyvault.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document(collection="adminAuditLogs")
public class AdminAuditLog {
 @Id private String id; @Indexed private String actorEmail; private String method; private String path;
 private int status; private String ipAddress; private String userAgent; @Builder.Default @Indexed private Instant timestamp=Instant.now();
}
