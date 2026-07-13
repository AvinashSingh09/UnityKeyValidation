package com.company.keyvault.service;

import com.company.keyvault.dto.request.*;
import com.company.keyvault.dto.response.UserResponse;
import com.company.keyvault.exception.*;
import com.company.keyvault.model.User;
import com.company.keyvault.model.enums.UserRole;
import com.company.keyvault.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UserManagementService {
 private final UserRepository users; private final PasswordEncoder encoder; private final SessionService sessions;
 public UserManagementService(UserRepository users,PasswordEncoder encoder,SessionService sessions){this.users=users;this.encoder=encoder;this.sessions=sessions;}
 public List<UserResponse> list(){return users.findAll().stream().sorted(Comparator.comparing(User::getCreatedAt,Comparator.nullsLast(Comparator.naturalOrder()))).map(UserResponse::from).toList();}
 public UserResponse create(UserCreateRequest request){String email=request.getEmail().toLowerCase();if(users.existsByEmail(email))throw new DuplicateResourceException("User already exists with this email");User user=users.save(User.builder().email(email).fullName(request.getFullName()).passwordHash(encoder.encode(request.getPassword())).role(request.getRole()).active(true).build());return UserResponse.from(user);}
 public UserResponse update(String id,UserUpdateRequest request,String actor){User user=find(id);if(user.getEmail().equalsIgnoreCase(actor)&&Boolean.FALSE.equals(request.getActive()))throw new AccessDeniedException("You cannot deactivate your own account");boolean removesSuper=user.getRole()==UserRole.SUPER_ADMIN&&(Boolean.FALSE.equals(request.getActive())||(request.getRole()!=null&&request.getRole()!=UserRole.SUPER_ADMIN));long activeSuperAdmins=users.findAll().stream().filter(candidate->candidate.getRole()==UserRole.SUPER_ADMIN&&candidate.isActive()).count();if(removesSuper&&activeSuperAdmins<=1)throw new AccessDeniedException("At least one active super admin is required");if(request.getRole()!=null)user.setRole(request.getRole());if(request.getActive()!=null)user.setActive(request.getActive());user=users.save(user);if(!user.isActive())sessions.revokeAll(user.getId());return UserResponse.from(user);}
 public void changePassword(String email,ChangePasswordRequest request){User user=users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));if(!encoder.matches(request.getCurrentPassword(),user.getPasswordHash()))throw new BadCredentialsException("Current password is incorrect");if(encoder.matches(request.getNewPassword(),user.getPasswordHash()))throw new IllegalArgumentException("New password must be different");user.setPasswordHash(encoder.encode(request.getNewPassword()));users.save(user);sessions.revokeAll(user.getId());}
 public User findByEmail(String email){return users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));}
 private User find(String id){return users.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found"));}
}
