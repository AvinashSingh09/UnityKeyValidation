package com.company.keyvault.service;

import com.company.keyvault.dto.response.SecuritySessionResponse;
import com.company.keyvault.model.RefreshSession;
import com.company.keyvault.model.User;
import com.company.keyvault.repository.RefreshSessionRepository;
import com.company.keyvault.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class SessionService {
 private final RefreshSessionRepository repository; private final JwtTokenProvider tokens;
 public SessionService(RefreshSessionRepository repository,JwtTokenProvider tokens){this.repository=repository;this.tokens=tokens;}
 public IssuedSession issue(User user,String ip,String agent){String token=tokens.generateRefreshToken(user.getEmail()); RefreshSession session=repository.save(RefreshSession.builder().userId(user.getId()).tokenHash(hash(token)).ipAddress(ip).userAgent(agent).lastUsedAt(Instant.now()).expiresAt(tokens.getExpiration(token)).build()); return new IssuedSession(token,session.getId());}
 public synchronized IssuedSession rotate(String token,String ip,String agent){if(token==null||!tokens.validateToken(token)||!tokens.isRefreshToken(token))throw new BadCredentialsException("Invalid refresh token"); RefreshSession old=repository.findByTokenHash(hash(token)).filter(s->s.getRevokedAt()==null&&s.getExpiresAt().isAfter(Instant.now())).orElseThrow(()->new BadCredentialsException("Invalid refresh token")); old.setRevokedAt(Instant.now()); old.setLastUsedAt(Instant.now()); repository.save(old); String next=tokens.generateRefreshToken(tokens.getEmailFromToken(token)); RefreshSession created=repository.save(RefreshSession.builder().userId(old.getUserId()).tokenHash(hash(next)).ipAddress(ip).userAgent(agent).lastUsedAt(Instant.now()).expiresAt(tokens.getExpiration(next)).build()); return new IssuedSession(next,created.getId());}
 public void revokeToken(String token){if(token==null)return;repository.findByTokenHash(hash(token)).ifPresent(s->{s.setRevokedAt(Instant.now());repository.save(s);});}
 public void revokeSession(String userId,String sessionId){repository.findById(sessionId).filter(s->s.getUserId().equals(userId)).ifPresent(s->{s.setRevokedAt(Instant.now());repository.save(s);});}
 public void revokeAll(String userId){repository.findByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(userId).forEach(s->{s.setRevokedAt(Instant.now());repository.save(s);});}
 public List<SecuritySessionResponse> list(String userId){return repository.findByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(userId).stream().filter(s->s.getExpiresAt().isAfter(Instant.now())).map(SecuritySessionResponse::from).toList();}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public record IssuedSession(String token,String sessionId){}
}
