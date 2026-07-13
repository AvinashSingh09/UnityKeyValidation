package com.company.keyvault.security;

import com.company.keyvault.model.AdminAuditLog; import com.company.keyvault.repository.AdminAuditLogRepository;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.slf4j.*; import org.springframework.security.core.*; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException; import java.util.Set;

@Component
public class AuditLogFilter extends OncePerRequestFilter {
 private static final Logger log=LoggerFactory.getLogger(AuditLogFilter.class); private static final Set<String> READ=Set.of("GET","HEAD","OPTIONS"); private final AdminAuditLogRepository repository;
 public AuditLogFilter(AdminAuditLogRepository repository){this.repository=repository;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{chain.doFilter(req,res);Authentication auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!auth.isAuthenticated()||"anonymousUser".equals(auth.getPrincipal())||READ.contains(req.getMethod())||req.getRequestURI().endsWith("/auth/refresh")||req.getRequestURI().endsWith("/auth/login"))return;try{repository.save(AdminAuditLog.builder().actorEmail(auth.getName()).method(req.getMethod()).path(req.getRequestURI()).status(res.getStatus()).ipAddress(RequestMetadata.ip(req)).userAgent(RequestMetadata.userAgent(req)).build());}catch(Exception e){log.error("Failed to write admin audit log",e);}}
}
