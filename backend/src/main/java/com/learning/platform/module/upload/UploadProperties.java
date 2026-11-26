package com.learning.platform.module.upload;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Data
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private DataSize partSize = DataSize.ofMegabytes(10);
    private DataSize maxVideoSize = DataSize.ofGigabytes(2);
    private DataSize maxDocSize = DataSize.ofMegabytes(100);
    private DataSize maxCoverSize = DataSize.ofMegabytes(5);
    /** 分片上传 URL 有效期（秒）。 */
    private long partUrlExpiresSeconds = 3600;
    /** 单次最多签发多少个分片 URL。 */
    private int maxPartUrlsPerRequest = 100;
}
