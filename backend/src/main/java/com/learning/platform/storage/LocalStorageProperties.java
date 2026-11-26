package com.learning.platform.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/** 仅供关闭 OBS 的本地开发环境使用。 */
@Data
@ConfigurationProperties(prefix = "app.local-storage")
public class LocalStorageProperties {
    private Path root = Path.of(System.getProperty("java.io.tmpdir"), "learning-platform-storage");
}
