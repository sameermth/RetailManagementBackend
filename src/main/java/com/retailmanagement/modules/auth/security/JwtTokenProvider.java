package com.retailmanagement.modules.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserPrincipal principal = userDetails instanceof UserPrincipal userPrincipal ? userPrincipal : null;

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_")) // Only get roles
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        if (principal != null) {
            claims.put("organizationId", principal.getOrganizationId());
            claims.put("subscriptionVersion", principal.getSubscriptionVersion());
            claims.put("subscriptionPlanCode", principal.getSubscriptionPlanCode());
            claims.put("subscriptionStatus", principal.getSubscriptionStatus());
            claims.put("subscriptionFeatures", principal.getSubscriptionFeatures());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isSubscriptionContextValid(String token, UserPrincipal principal) {
        Claims claims = getClaims(token);
        Number orgId = claims.get("organizationId", Number.class);
        Number subscriptionVersion = claims.get("subscriptionVersion", Number.class);
        if (principal == null) {
            return false;
        }
        return orgId != null
                && subscriptionVersion != null
                && principal.getOrganizationId() != null
                && principal.getSubscriptionVersion() != null
                && principal.getOrganizationId().equals(orgId.longValue())
                && principal.getSubscriptionVersion().equals(subscriptionVersion.longValue());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
