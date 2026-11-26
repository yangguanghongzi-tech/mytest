package com.learning.platform.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.obs")
public class ObsProperties {
    /** 设为 false 时不创建 OBS 客户端（测试中用内存实现替代）。 */
    private boolean enabled = true;
    /** 例如 https://obs.cn-north-4.myhuaweicloud.com */
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
