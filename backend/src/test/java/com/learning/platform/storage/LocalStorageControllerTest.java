package com.learning.platform.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageControllerTest {

    @TempDir
    Path root;

    @Test
    void returnsMediaTypeFromObjectFileExtension() throws Exception {
        Path object = root.resolve("objects/t1/cover/example.png");
        Files.createDirectories(object.getParent());
        Files.write(object, new byte[]{1, 2, 3});

        LocalStorageProperties properties = new LocalStorageProperties();
        properties.setRoot(root);
        LocalStorageController controller = new LocalStorageController(new LocalStorageService(properties));
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/local-storage/objects/t1/cover/example.png");

        assertThat(controller.object(request, null).getHeaders().getContentType())
                .isEqualTo(MediaType.IMAGE_PNG);
    }
}
