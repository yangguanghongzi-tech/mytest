package com.learning.platform.auth;

/**
 * 把 token 解析成 Principal。token 无效时抛出 InvalidTokenException。
 * 统一站点的 token 格式还没最终确定：换格式时只需要替换实现（JWT 本地验签、远程 introspect 等），业务代码不用改。
 */
public interface TokenResolver {

    Principal resolve(String token) throws InvalidTokenException;
}
