package com.learning.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 对 /admin/api/** 和 /h5/api/** 做鉴权：解析 Bearer token，写入 TenantContext，请求结束后清理。
 * /admin/api/** 还要求具有管理员角色。
 */
@Slf4j
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final TokenResolver tokenResolver;
    private final AuthProperties props;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true; // CORS 预检
        }
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        return !(uri.startsWith("/admin/api/") || uri.startsWith("/h5/api/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER) || header.length() == BEARER.length()) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return;
        }
        Principal principal;
        try {
            principal = tokenResolver.resolve(header.substring(BEARER.length()).trim());
        } catch (InvalidTokenException e) {
            log.debug("token 无效: {}", e.getMessage());
            write(response, HttpServletResponse.SC_UNAUTHORIZED, "登录已失效");
            return;
        }

        String uri = request.getRequestURI().substring(request.getContextPath().length());
        if (uri.startsWith("/admin/api/") && !principal.hasRole(props.getAdminRole())) {
            write(response, HttpServletResponse.SC_FORBIDDEN, "无后台管理权限");
            return;
        }

        TenantContext.set(principal);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Result.fail(status, message));
    }
}
