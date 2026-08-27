package com.example.paymenthub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, TokenBlacklistService tokenBlacklistService) {
        this.jwtProvider = jwtProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);
            if (jwt != null && !tokenBlacklistService.isBlacklisted(jwt) && jwtProvider.validateToken(jwt)) {
                String tokenType = jwtProvider.getTypeFromToken(jwt);
                if (com.example.paymenthub.common.enums.TokenType.ACCESS.getTypeName().equals(tokenType)) {
                    String username = jwtProvider.getUsernameFromToken(jwt);
                    String role = jwtProvider.getRoleFromToken(jwt);
                    List<String> permissions = jwtProvider.getPermissionsFromToken(jwt);

                    List<GrantedAuthority> authorities = new ArrayList<>();

                    // 1. Nạp vai trò (ROLE_MAKER, ROLE_CHECKER...)
                    if (StringUtils.hasText(role)) {
                        String authorityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        authorities.add(new SimpleGrantedAuthority(authorityRole));
                    }

                    // 2. Nạp tất cả các Chức năng nhỏ (CATEGORY_CREATE, CATEGORY_VIEW...)
                    if (permissions != null) {
                        for (String perm : permissions) {
                            if (StringUtils.hasText(perm)) {
                                authorities.add(new SimpleGrantedAuthority(perm));
                            }
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("[JWT Filter] Token bị từ chối do không phải Access Token (type={})", tokenType);
                }
            }
        } catch (Exception e) {
            log.error("[JWT Filter] Không thể thiết lập xác thực người dùng: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (com.example.paymenthub.common.enums.TokenType.ACCESS.getCookieName().equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
