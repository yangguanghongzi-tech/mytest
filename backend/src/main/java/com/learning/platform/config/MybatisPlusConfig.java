package com.learning.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.learning.platform.auth.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Set;

@Configuration
public class MybatisPlusConfig {

    /** 不做租户过滤的表（全局表）。目前没有，所有业务表都按租户隔离。 */
    private static final Set<String> IGNORE_TABLES = Set.of();

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 租户插件必须放在分页插件前面
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new StringValue(TenantContext.requireTenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TABLES.contains(tableName.toLowerCase());
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler auditFieldHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject meta) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(meta, "createdAt", LocalDateTime.class, now);
                strictInsertFill(meta, "updatedAt", LocalDateTime.class, now);
                strictInsertFill(meta, "createdBy", String.class, TenantContext.currentUserId());
            }

            @Override
            public void updateFill(MetaObject meta) {
                // 强制覆盖，否则实体里已有的旧值会让 strictUpdateFill 跳过填充
                setFieldValByName("updatedAt", LocalDateTime.now(), meta);
            }
        };
    }
}
