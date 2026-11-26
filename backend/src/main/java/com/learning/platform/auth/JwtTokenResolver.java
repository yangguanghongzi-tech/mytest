package com.learning.platform.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 用统一站点的公钥（或 JWKS / HMAC 密钥）在本地校验 JWT，并按配置的字段名读取 claim。 */
@Component
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "jwt", matchIfMissing = true)
public class JwtTokenResolver implements TokenResolver {

    private final AuthProperties.Jwt cfg;
    private final DefaultJWTProcessor<SecurityContext> processor;

    public JwtTokenResolver(AuthProperties props) throws Exception {
        this.cfg = props.getJwt();
        JWSAlgorithm alg = JWSAlgorithm.parse(cfg.getAlgorithm());

        JWKSource<SecurityContext> keySource;
        if (StringUtils.hasText(cfg.getJwksUrl())) {
            keySource = JWKSourceBuilder.create(new URL(cfg.getJwksUrl())).build();
        } else if (StringUtils.hasText(cfg.getPublicKeyPem())) {
            keySource = new ImmutableJWKSet<>(new JWKSet(JWK.parseFromPEMEncodedObjects(cfg.getPublicKeyPem())));
        } else if (StringUtils.hasText(cfg.getHmacSecret())) {
            keySource = new ImmutableSecret<>(cfg.getHmacSecret().getBytes(StandardCharsets.UTF_8));
        } else {
            throw new IllegalStateException("app.auth.mode=jwt 时必须配置 jwks-url、public-key-pem 或 hmac-secret 之一");
        }

        processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(alg, keySource));

        JWTClaimsSet.Builder exact = new JWTClaimsSet.Builder();
        if (StringUtils.hasText(cfg.getIssuer())) {
            exact.issuer(cfg.getIssuer());
        }
        DefaultJWTClaimsVerifier<SecurityContext> verifier = new DefaultJWTClaimsVerifier<>(
                exact.build(), Set.of("exp", cfg.getClaims().getTenantId()));
        verifier.setMaxClockSkew(cfg.getClockSkewSeconds());
        processor.setJWTClaimsSetVerifier(verifier);
    }

    @Override
    public Principal resolve(String token) throws InvalidTokenException {
        JWTClaimsSet claims;
        try {
            claims = processor.process(token, null);
        } catch (Exception e) {
            throw new InvalidTokenException("token 校验失败", e);
        }
        AuthProperties.Claims names = cfg.getClaims();
        String tenantId = stringClaim(claims, names.getTenantId());
        if (!StringUtils.hasText(tenantId)) {
            throw new InvalidTokenException("token 中缺少租户信息");
        }
        String userId = stringClaim(claims, names.getUserId());
        return new Principal(tenantId, userId, rolesClaim(claims.getClaim(names.getRoles())));
    }

    private static String stringClaim(JWTClaimsSet claims, String name) {
        Object v = claims.getClaim(name);
        return v == null ? null : String.valueOf(v);
    }

    /** 兼容数组形式（["admin"]）和逗号分隔字符串形式（"admin,editor"）。 */
    private static Set<String> rolesClaim(Object v) {
        Set<String> roles = new HashSet<>();
        if (v instanceof Collection<?> c) {
            c.forEach(r -> roles.add(String.valueOf(r)));
        } else if (v instanceof String s && !s.isBlank()) {
            roles.addAll(List.of(s.split("\\s*,\\s*")));
        }
        return roles;
    }
}
