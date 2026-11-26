package com.learning.platform.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地开发存储。分片和合并后的对象写到本机目录，使开发环境不配置 OBS 也能完整联调上传。
 * 上传任务元数据只保存在进程内；后端重启后应重新选择文件开始上传。
 */
@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "app.obs", name = "enabled", havingValue = "false")
public class LocalStorageService implements StorageService {

    private final Path root;
    private final Map<String, String> uploadKeys = new ConcurrentHashMap<>();

    public LocalStorageService(LocalStorageProperties props) throws IOException {
        root = props.getRoot().toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("uploads"));
        Files.createDirectories(root.resolve("objects"));
    }

    @Override
    public String initMultipart(String objectKey, String contentType) {
        String uploadId = UUID.randomUUID().toString();
        uploadKeys.put(uploadId, objectKey);
        try {
            Files.createDirectories(uploadDir(uploadId));
        } catch (IOException e) {
            uploadKeys.remove(uploadId);
            throw storageError("创建本地上传任务失败", e);
        }
        return uploadId;
    }

    @Override
    public String signPartUploadUrl(String objectKey, String uploadId, int partNumber, long expiresSeconds) {
        requireUpload(uploadId, objectKey);
        return "/local-storage/parts/" + uploadId + "/" + partNumber;
    }

    @Override
    public List<PartInfo> listParts(String objectKey, String uploadId) {
        requireUpload(uploadId, objectKey);
        List<PartInfo> result = new ArrayList<>();
        try (var paths = Files.list(uploadDir(uploadId))) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    int number = Integer.parseInt(path.getFileName().toString());
                    result.add(new PartInfo(number, "local-" + number, Files.size(path)));
                } catch (IOException | NumberFormatException e) {
                    throw storageError("读取本地分片失败", e);
                }
            });
        } catch (IOException e) {
            throw storageError("读取本地分片失败", e);
        }
        result.sort(Comparator.comparingInt(PartInfo::partNumber));
        return result;
    }

    @Override
    public void completeMultipart(String objectKey, String uploadId, List<PartInfo> parts) {
        requireUpload(uploadId, objectKey);
        Path target = objectPath(objectKey);
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(temporary)) {
                for (PartInfo part : parts.stream().sorted(Comparator.comparingInt(PartInfo::partNumber)).toList()) {
                    Files.copy(uploadDir(uploadId).resolve(String.valueOf(part.partNumber())), out);
                }
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            deleteDirectory(uploadDir(uploadId));
            uploadKeys.remove(uploadId);
        } catch (IOException e) {
            throw storageError("合并本地分片失败", e);
        }
    }

    @Override
    public void abortMultipart(String objectKey, String uploadId) {
        requireUpload(uploadId, objectKey);
        try {
            deleteDirectory(uploadDir(uploadId));
            uploadKeys.remove(uploadId);
        } catch (IOException e) {
            throw storageError("取消本地上传失败", e);
        }
    }

    @Override
    public long objectSize(String objectKey) {
        try {
            Path path = objectPath(objectKey);
            return Files.exists(path) ? Files.size(path) : -1;
        } catch (IOException e) {
            throw storageError("读取本地文件失败", e);
        }
    }

    @Override
    public String signGetUrl(String objectKey, long expiresSeconds, String downloadFileName) {
        String url = "/local-storage/objects/" + UriUtils.encodePath(objectKey, StandardCharsets.UTF_8);
        return downloadFileName == null ? url : url + "?download="
                + UriUtils.encodeQueryParam(downloadFileName, StandardCharsets.UTF_8);
    }

    public void putPart(String uploadId, int partNumber, InputStream data) {
        if (partNumber < 1) {
            throw new IllegalArgumentException("分片号不正确");
        }
        requireUpload(uploadId, null);
        Path target = uploadDir(uploadId).resolve(String.valueOf(partNumber));
        try {
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw storageError("写入本地分片失败", e);
        }
    }

    public Path getObject(String objectKey) {
        Path path = objectPath(objectKey);
        return Files.isRegularFile(path) ? path : null;
    }

    private void requireUpload(String uploadId, String expectedObjectKey) {
        String actual = uploadKeys.get(uploadId);
        if (actual == null || expectedObjectKey != null && !actual.equals(expectedObjectKey)) {
            throw new IllegalArgumentException("本地上传任务不存在或后端已重启");
        }
    }

    private Path uploadDir(String uploadId) {
        if (!uploadId.matches("[0-9a-f-]{36}")) {
            throw new IllegalArgumentException("上传任务编号不正确");
        }
        return root.resolve("uploads").resolve(uploadId);
    }

    private Path objectPath(String objectKey) {
        Path base = root.resolve("objects").normalize();
        Path path = base.resolve(objectKey).normalize();
        if (!path.startsWith(base)) {
            throw new IllegalArgumentException("文件路径不正确");
        }
        return path;
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static IllegalStateException storageError(String message, Exception cause) {
        return new IllegalStateException(message, cause);
    }
}
