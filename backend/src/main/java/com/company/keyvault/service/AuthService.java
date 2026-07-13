package com.company.keyvault.service;

import com.company.keyvault.dto.request.LoginRequest;
import com.company.keyvault.dto.request.RegisterRequest;
import com.company.keyvault.dto.response.AuthResponse;
import com.company.keyvault.exception.DuplicateResourceException;
import com.company.keyvault.model.User;
import com.company.keyvault.model.enums.UserRole;
import com.company.keyvault.repository.UserRepository;
import com.company.keyvault.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SessionService sessionService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.sessionService = sessionService;
    }

    public synchronized AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Public registration is disabled. Ask a super admin to create your account.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(UserRole.SUPER_ADMIN)
                .active(true)
                .lastLoginAt(Instant.now())
                .build();

        user = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user.getEmail());
        SessionService.IssuedSession session = sessionService.issue(user, ipAddress, userAgent);

        return AuthResponse.of(accessToken, session.token(), session.sessionId(),
                user.getEmail(), user.getFullName(), user.getRole());
    }

    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(), request.getPassword()));

        String accessToken = tokenProvider.generateAccessToken(authentication);
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        SessionService.IssuedSession session = sessionService.issue(user, ipAddress, userAgent);

        return AuthResponse.of(accessToken, session.token(), session.sessionId(),
                user.getEmail(), user.getFullName(), user.getRole());
    }

    public AuthResponse refreshToken(String refreshToken, String ipAddress, String userAgent) {
        SessionService.IssuedSession session = sessionService.rotate(refreshToken, ipAddress, userAgent);
        String email = tokenProvider.getEmailFromToken(session.token());
        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid refresh token"));

        String newAccessToken = tokenProvider.generateAccessToken(email);
        return AuthResponse.of(newAccessToken, session.token(), session.sessionId(),
                user.getEmail(), user.getFullName(), user.getRole());
    }

    public boolean isSetupRequired() { return !userRepository.existsByRole(UserRole.SUPER_ADMIN); }

    public void logout(String refreshToken) { sessionService.revokeToken(refreshToken); }
}
