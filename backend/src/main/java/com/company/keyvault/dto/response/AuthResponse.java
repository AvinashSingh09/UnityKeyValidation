package com.company.keyvault.dto.response;

import com.company.keyvault.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String sessionId;
    private String email;
    private String fullName;
    private UserRole role;

    public static AuthResponse of(String accessToken, String refreshToken, String sessionId,
                                   String email, String fullName, UserRole role) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .sessionId(sessionId)
                .email(email)
                .fullName(fullName)
                .role(role)
                .build();
    }
}
