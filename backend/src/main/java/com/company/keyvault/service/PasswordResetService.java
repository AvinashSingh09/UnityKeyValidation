package com.company.keyvault.service;

import com.company.keyvault.model.*;
import com.company.keyvault.repository.*;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.security.SecureRandom;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class PasswordResetService {
 private static final Logger log=LoggerFactory.getLogger(PasswordResetService.class); private final UserRepository users; private final PasswordResetTokenRepository resets; private final PasswordEncoder encoder; private final SessionService sessions; private final ObjectProvider<JavaMailSender> mail;
 @Value("${app.security.password-reset-url:http://localhost:5173/reset-password}") private String resetUrl; @Value("${app.security.mail-from:noreply@keyvault.local}") private String from; @Value("${app.security.expose-reset-link:false}") private boolean expose;
 public PasswordResetService(UserRepository users,PasswordResetTokenRepository resets,PasswordEncoder encoder,SessionService sessions,ObjectProvider<JavaMailSender> mail){this.users=users;this.resets=resets;this.encoder=encoder;this.sessions=sessions;this.mail=mail;}
 @Async public void request(String email){users.findByEmail(email.toLowerCase()).filter(User::isActive).ifPresent(user->{resets.findByUserIdAndUsedAtIsNull(user.getId()).forEach(previous->{previous.setUsedAt(Instant.now());resets.save(previous);});byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);resets.save(PasswordResetToken.builder().userId(user.getId()).tokenHash(SessionService.hash(token)).expiresAt(Instant.now().plus(30,ChronoUnit.MINUTES)).build());String link=resetUrl+"?token="+token;JavaMailSender sender=mail.getIfAvailable();if(sender!=null){try{SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(user.getEmail());message.setSubject("Reset your KeyVault password");message.setText("Use this link within 30 minutes to reset your password:\n\n"+link);sender.send(message);}catch(Exception e){log.error("Password reset email delivery failed",e);}}else if(expose){log.warn("Development password reset link: {}",link);}});}
 public void reset(String token,String newPassword){PasswordResetToken reset=resets.findByTokenHash(SessionService.hash(token)).filter(r->r.getUsedAt()==null&&r.getExpiresAt().isAfter(Instant.now())).orElseThrow(()->new BadCredentialsException("Invalid or expired reset token"));User user=users.findById(reset.getUserId()).orElseThrow(()->new BadCredentialsException("Invalid reset token"));user.setPasswordHash(encoder.encode(newPassword));users.save(user);resets.findByUserIdAndUsedAtIsNull(user.getId()).forEach(pending->{pending.setUsedAt(Instant.now());resets.save(pending);});sessions.revokeAll(user.getId());}
}
