package com.learning.platform.auth;

import java.util.Set;

/** 当前请求的身份信息，只能从已验证的 token 中得到。 */
public record Principal(String tenantId, String userId, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
