package com.company.keyvault.security;
import jakarta.servlet.http.HttpServletRequest;
public final class RequestMetadata {
 private RequestMetadata() {}
 public static String ip(HttpServletRequest request){String forwarded=request.getHeader("X-Forwarded-For"); return forwarded==null||forwarded.isBlank()?request.getRemoteAddr():forwarded.split(",")[0].trim();}
 public static String userAgent(HttpServletRequest request){String value=request.getHeader("User-Agent"); return value==null?"Unknown":value.substring(0,Math.min(value.length(),300));}
}
