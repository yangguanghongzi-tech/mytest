package com.learning.platform.module.content;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.content")
public class ContentProperties {
    /** 播放 / 预览 / 下载签名 URL 有效期（秒）。 */
    private long urlExpiresSeconds = 7200;
}
