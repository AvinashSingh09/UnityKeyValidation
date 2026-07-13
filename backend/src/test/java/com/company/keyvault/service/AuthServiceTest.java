package com.company.keyvault.service;

import com.company.keyvault.dto.request.RegisterRequest;
import com.company.keyvault.dto.response.AuthResponse;
import com.company.keyvault.model.User;
import com.company.keyvault.model.enums.UserRole;
import com.company.keyvault.repository.UserRepository;
import com.company.keyvault.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private UserRepository users; private PasswordEncoder encoder; private SessionService sessions; private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class); encoder = mock(PasswordEncoder.class);
        sessions = mock(SessionService.class);
        JwtTokenProvider tokens = new JwtTokenProvider("test-secret", 60_000, 120_000);
        service = new AuthService(users, encoder, mock(AuthenticationManager.class), tokens, sessions);
    }

    @Test
    void onlyFirstAccountCanUsePublicRegistrationAndBecomesOwner() {
        when(users.existsByRole(UserRole.SUPER_ADMIN)).thenReturn(false);
        when(users.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode("strong-password")).thenReturn("hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> { User user=invocation.getArgument(0); user.setId("user-1"); return user; });
        when(sessions.issue(any(), anyString(), anyString())).thenReturn(new SessionService.IssuedSession("refresh", "session-1"));
        RegisterRequest request = new RegisterRequest("Owner", "OWNER@EXAMPLE.COM", "strong-password");

        AuthResponse response = service.register(request, "127.0.0.1", "test");

        assertEquals(UserRole.SUPER_ADMIN, response.getRole());
        assertEquals("owner@example.com", response.getEmail());
    }

    @Test
    void publicRegistrationIsDisabledAfterOwnerExists() {
        when(users.existsByRole(UserRole.SUPER_ADMIN)).thenReturn(true);
        assertThrows(AccessDeniedException.class,
                () -> service.register(new RegisterRequest("User", "user@example.com", "strong-password"), "127.0.0.1", "test"));
        verify(users, never()).save(any());
    }
}
