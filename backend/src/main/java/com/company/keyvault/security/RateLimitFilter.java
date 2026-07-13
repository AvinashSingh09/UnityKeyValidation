package com.company.keyvault.security;

import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException; import java.time.*; import java.util.concurrent.*;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
 private final ConcurrentMap<String,Window> windows=new ConcurrentHashMap<>();
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  Limit limit=limit(req);if(limit!=null){String key=req.getRequestURI()+":"+RequestMetadata.ip(req);Window window=windows.compute(key,(k,current)->current==null||current.started.plus(limit.duration).isBefore(Instant.now())?new Window(Instant.now(),1):new Window(current.started,current.count+1));if(window.count>limit.requests){res.setStatus(429);res.setContentType("application/json");res.setHeader("Retry-After",String.valueOf(limit.duration.toSeconds()));res.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Please wait before trying again.\"}");return;}}
  chain.doFilter(req,res);
 }
 private Limit limit(HttpServletRequest req){if(!"POST".equals(req.getMethod()))return null;String p=req.getRequestURI();if(p.endsWith("/auth/login"))return new Limit(10,Duration.ofMinutes(15));if(p.endsWith("/auth/register"))return new Limit(5,Duration.ofHours(1));if(p.endsWith("/auth/forgot-password"))return new Limit(5,Duration.ofMinutes(15));if(p.startsWith("/api/validate/"))return new Limit(120,Duration.ofMinutes(1));return null;}
 private record Window(Instant started,int count){} private record Limit(int requests,Duration duration){}
}
