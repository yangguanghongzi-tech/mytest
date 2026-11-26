package com.learning.platform.auth;

import com.learning.platform.common.BizException;

/** 保存当前请求的 Principal。由 TokenAuthFilter 设置，请求结束时清理。 */
public final class TenantContext {

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Principal principal) {
        HOLDER.set(principal);
    }

    public static Principal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 没有租户上下文时直接拒绝，避免出现不带租户条件的 SQL。 */
    public static String requireTenantId() {
        Principal p = HOLDER.get();
        if (p == null || p.tenantId() == null || p.tenantId().isBlank()) {
            throw BizException.unauthorized("缺少租户上下文");
        }
        return p.tenantId();
    }

    public static String currentUserId() {
        Principal p = HOLDER.get();
        return p == null ? null : p.userId();
    }

    public static String requireUserId() {
        String userId = currentUserId();
        if (userId == null || userId.isBlank()) {
            throw BizException.unauthorized("缺少用户上下文");
        }
        return userId;
    }
}
