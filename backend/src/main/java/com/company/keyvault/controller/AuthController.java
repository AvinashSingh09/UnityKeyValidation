package com.company.keyvault.controller;

import com.company.keyvault.dto.request.LoginRequest;
import com.company.keyvault.dto.request.RegisterRequest;
import com.company.keyvault.dto.response.AuthResponse;
import com.company.keyvault.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import com.company.keyvault.dto.request.ForgotPasswordRequest;
import com.company.keyvault.dto.request.ResetPasswordRequest;
import com.company.keyvault.dto.response.MessageResponse;
import com.company.keyvault.security.RequestMetadata;
import com.company.keyvault.service.PasswordResetService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        AuthResponse response = authService.register(request, RequestMetadata.ip(http), RequestMetadata.userAgent(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthResponse response = authService.login(request, RequestMetadata.ip(http), RequestMetadata.userAgent(http));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> request, HttpServletRequest http) {
        String refreshToken = request.get("refreshToken");
        AuthResponse response = authService.refreshToken(refreshToken, RequestMetadata.ip(http), RequestMetadata.userAgent(http));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/setup-status")
    public ResponseEntity<Map<String, Boolean>> setupStatus() { return ResponseEntity.ok(Map.of("setupRequired", authService.isSetupRequired())); }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestBody Map<String,String> request) { authService.logout(request.get("refreshToken")); return ResponseEntity.ok(MessageResponse.of("Signed out")); }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgot(@Valid @RequestBody ForgotPasswordRequest request) { passwordResetService.request(request.getEmail()); return ResponseEntity.ok(MessageResponse.of("If that account exists, a reset link has been sent.")); }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> reset(@Valid @RequestBody ResetPasswordRequest request) { passwordResetService.reset(request.getToken(),request.getNewPassword()); return ResponseEntity.ok(MessageResponse.of("Password reset successfully")); }
}
