package com.company.keyvault.controller;

import com.company.keyvault.dto.request.*;
import com.company.keyvault.dto.response.*;
import com.company.keyvault.model.AdminAuditLog;
import com.company.keyvault.repository.AdminAuditLogRepository;
import com.company.keyvault.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/security")
public class SecurityController {
 private final UserManagementService users; private final SessionService sessions; private final AdminAuditLogRepository audit;
 public SecurityController(UserManagementService users,SessionService sessions,AdminAuditLogRepository audit){this.users=users;this.sessions=sessions;this.audit=audit;}
 @GetMapping("/sessions") public List<SecuritySessionResponse> sessions(Authentication auth){return sessions.list(users.findByEmail(auth.getName()).getId());}
 @DeleteMapping("/sessions/{id}") public ResponseEntity<MessageResponse> revoke(@PathVariable String id,Authentication auth){sessions.revokeSession(users.findByEmail(auth.getName()).getId(),id);return ResponseEntity.ok(MessageResponse.of("Session revoked"));}
 @PostMapping("/change-password") public ResponseEntity<MessageResponse> change(@Valid @RequestBody ChangePasswordRequest request,Authentication auth){users.changePassword(auth.getName(),request);return ResponseEntity.ok(MessageResponse.of("Password changed. Sign in again on this device."));}
 @GetMapping("/users") @PreAuthorize("hasRole('SUPER_ADMIN')") public List<UserResponse> listUsers(){return users.list();}
 @PostMapping("/users") @PreAuthorize("hasRole('SUPER_ADMIN')") public UserResponse createUser(@Valid @RequestBody UserCreateRequest request){return users.create(request);}
 @PutMapping("/users/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')") public UserResponse updateUser(@PathVariable String id,@RequestBody UserUpdateRequest request,Authentication auth){return users.update(id,request,auth.getName());}
 @GetMapping("/audit") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public Page<AdminAuditLog> audit(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="30")int size){return audit.findAllByOrderByTimestampDesc(PageRequest.of(page,Math.min(size,100)));}
}
