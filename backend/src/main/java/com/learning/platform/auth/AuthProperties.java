package com.learning.platform.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /** jwt：生产环境；dev：仅本地开发和测试，接受 dev:{tenant}:{user}:{roles} 格式的伪 token。 */
    private String mode = "jwt";

    /** 具有后台管理权限的角色名。 */
    private String adminRole = "admin";

    private Jwt jwt = new Jwt();

    @Data
    public static class Jwt {
        /** JWS 算法，例如 RS256、HS256。 */
        private String algorithm = "RS256";
        /** 三选一：JWKS 地址 / PEM 公钥 / HMAC 密钥。 */
        private String jwksUrl;
        private String publicKeyPem;
        private String hmacSecret;
        /** 不为空时校验 iss。 */
        private String issuer;
        /** 允许的时钟偏差（秒）。 */
        private int clockSkewSeconds = 60;
        private Claims claims = new Claims();
    }

    /** claim 字段名映射，等统一站点确定后改配置即可。 */
    @Data
    public static class Claims {
        private String tenantId = "tenantId";
        private String userId = "sub";
        private String roles = "roles";
    }
}
