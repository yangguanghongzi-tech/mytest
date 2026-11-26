package com.learning.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.learning.platform.auth.AuthProperties;
import com.learning.platform.auth.TokenAuthFilter;
import com.learning.platform.auth.TokenResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Bean
    public FilterRegistrationBean<TokenAuthFilter> tokenAuthFilter(TokenResolver resolver, AuthProperties props,
                                                                   ObjectMapper objectMapper) {
        FilterRegistrationBean<TokenAuthFilter> bean =
                new FilterRegistrationBean<>(new TokenAuthFilter(resolver, props, objectMapper));
        bean.addUrlPatterns("/admin/api/*", "/h5/api/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return bean;
    }

    /**
     * 雪花 ID 超出 JS Number 精度：所有 Long（包装类型，即各类 ID）序列化为字符串。
     * 基本类型 long（如分页 total）保持数字。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToString() {
        return b -> b.serializerByType(Long.class, ToStringSerializer.instance);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
