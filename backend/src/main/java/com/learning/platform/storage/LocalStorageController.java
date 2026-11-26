package com.learning.platform.storage;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** 本地存储的直传与读取端点，仅在关闭 OBS 时存在。 */
@RestController
@Profile("dev")
@RequestMapping("/local-storage")
@ConditionalOnProperty(prefix = "app.obs", name = "enabled", havingValue = "false")
public class LocalStorageController {

    private final LocalStorageService storage;

    public LocalStorageController(LocalStorageService storage) {
        this.storage = storage;
    }

    @PutMapping("/parts/{uploadId}/{partNumber}")
    public void putPart(@PathVariable String uploadId, @PathVariable int partNumber,
                        HttpServletRequest request) throws IOException {
        storage.putPart(uploadId, partNumber, request.getInputStream());
    }

    @GetMapping("/objects/**")
    public ResponseEntity<FileSystemResource> object(HttpServletRequest request,
                                                      @RequestParam(required = false) String download) {
        String prefix = request.getContextPath() + "/local-storage/objects/";
        String objectKey = request.getRequestURI().substring(prefix.length());
        Path path = storage.getObject(objectKey);
        if (path == null) return ResponseEntity.notFound().build();

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentLength(path.toFile().length())
                .contentType(MediaTypeFactory.getMediaType(path.getFileName().toString())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM));
        if (download != null && !download.isBlank()) {
            response.header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(download, StandardCharsets.UTF_8).build().toString());
        }
        return response.body(new FileSystemResource(path));
    }
}
