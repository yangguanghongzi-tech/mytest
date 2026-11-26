package com.learning.platform.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 仅用于本地开发和测试：token 格式为 dev:{tenantId}:{userId}[:{role1,role2}]。
 * 只有 app.auth.mode=dev 时才会注册，生产环境不要开启。
 */
@Component
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "dev")
public class DevTokenResolver implements TokenResolver {

    @Override
    public Principal resolve(String token) throws InvalidTokenException {
        String[] parts = token.split(":", -1);
        if (parts.length < 3 || !"dev".equals(parts[0]) || parts[1].isBlank() || parts[2].isBlank()) {
            throw new InvalidTokenException("dev token 格式应为 dev:{tenantId}:{userId}[:{roles}]");
        }
        Set<String> roles = new HashSet<>();
        if (parts.length > 3 && !parts[3].isBlank()) {
            roles.addAll(Arrays.asList(parts[3].split(",")));
        }
        return new Principal(parts[1], parts[2], roles);
    }
}
