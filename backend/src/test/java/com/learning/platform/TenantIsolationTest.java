package com.learning.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationTest {

    static final String ADMIN_A = "dev:tenantA:ua:admin";
    static final String ADMIN_B = "dev:tenantB:ub:admin";
    static final String USER_A = "dev:tenantA:ua2";
    static final String USER_A_OTHER = "dev:tenantA:ua3";
    static final String USER_B = "dev:tenantB:ub2";
    static final long MB = 1024 * 1024;

    @TestConfiguration
    static class Cfg {
        @Bean
        InMemoryStorage storage() {
            return new InMemoryStorage();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired InMemoryStorage storage;

    // ---------- 鉴权 ----------

    @Test
    void rejectsMissingOrInvalidToken() throws Exception {
        mvc.perform(get("/h5/api/categories")).andExpect(status().isUnauthorized());
        mvc.perform(get("/h5/api/categories").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/api/categories").header("Authorization", "Basic xxx"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotUseAdminApi() throws Exception {
        call(USER_A, get("/admin/api/categories")).andExpect(status().isForbidden());
    }

    // ---------- 分类隔离 ----------

    @Test
    void categoriesAreIsolatedBetweenTenants() throws Exception {
        String idA = createCategory(ADMIN_A, "A-分类", null);

        JsonNode treeB = data(call(ADMIN_B, get("/admin/api/categories")));
        for (JsonNode n : treeB) {
            assertThat(n.get("id").asText()).isNotEqualTo(idA);
        }
        call(ADMIN_B, put("/admin/api/categories/" + idA).content("{\"name\":\"hack\"}"))
                .andExpect(status().isNotFound());
        call(ADMIN_B, delete("/admin/api/categories/" + idA)).andExpect(status().isNotFound());
        // B 不能把 A 的分类当作上级分类
        call(ADMIN_B, post("/admin/api/categories").content("{\"name\":\"x\",\"parentId\":\"" + idA + "\"}"))
                .andExpect(status().isBadRequest());

        // A 仍能看到且名称未被修改
        assertThat(data(call(ADMIN_A, get("/admin/api/categories"))).toString()).contains("A-分类");
    }

    @Test
    void categoryRules() throws Exception {
        String root = createCategory(ADMIN_A, "一级", null);
        String child = createCategory(ADMIN_A, "二级", root);
        call(ADMIN_A, post("/admin/api/categories").content("{\"name\":\"三级\",\"parentId\":\"" + child + "\"}"))
                .andExpect(status().isBadRequest());
        call(ADMIN_A, delete("/admin/api/categories/" + root)).andExpect(status().isConflict());
        call(ADMIN_A, post("/admin/api/categories").content("{\"name\":\"\"}")).andExpect(status().isBadRequest());
    }

    // ---------- 上传 + 内容 ----------

    @Test
    void videoUploadPublishAndH5View() throws Exception {
        String cat = createCategory(ADMIN_A, "视频课", null);
        String taskId = uploadFile(ADMIN_A, "VIDEO", "第一课.mp4", 25 * MB);

        String contentId = data(call(ADMIN_A, post("/admin/api/contents").content("""
                {"categoryId":"%s","type":"VIDEO","title":"第一课","fileTaskId":"%s","duration":600}
                """.formatted(cat, taskId)))).asText();

        // 草稿在 H5 不可见
        call(USER_A, get("/h5/api/contents/" + contentId)).andExpect(status().isNotFound());
        call(ADMIN_A, post("/admin/api/contents/" + contentId + "/publish")).andExpect(status().isOk());

        JsonNode detail = data(call(USER_A, get("/h5/api/contents/" + contentId)));
        assertThat(detail.at("/file/url").asText()).startsWith("https://obs.test/tenantA/video/");
        assertThat(detail.at("/file/duration").asInt()).isEqualTo(600);

        JsonNode list = data(call(USER_A, get("/h5/api/contents?categoryId=" + cat)));
        assertThat(list.get("total").asLong()).isEqualTo(1);
        assertThat(list.at("/records/0/id").asText()).isEqualTo(contentId);
        assertThat(list.at("/records/0/videoUrl").asText()).startsWith("https://obs.test/tenantA/video/");
        assertThat(list.at("/records/0/fileName").asText()).isEqualTo("第一课.mp4");
        assertThat(list.at("/records/0/fileSize").asLong()).isEqualTo(25 * MB);

        // 当前用户保存播放卡点后，详情和最近观看都返回该进度
        call(USER_A, put("/h5/api/contents/" + contentId + "/progress")
                .content("{\"progressSeconds\":125}"))
                .andExpect(status().isOk());
        detail = data(call(USER_A, get("/h5/api/contents/" + contentId)));
        assertThat(detail.get("progressSeconds").asInt()).isEqualTo(125);
        JsonNode recent = data(call(USER_A, get("/h5/api/contents/continue-watching")));
        assertThat(recent.get("contentId").asText()).isEqualTo(contentId);
        assertThat(recent.get("progressSeconds").asInt()).isEqualTo(125);
        assertThat(recent.get("progressPercent").asInt()).isEqualTo(20);

        // 播放器尚未初始化时可能上报 0，不能覆盖已经存在的有效卡点
        call(USER_A, put("/h5/api/contents/" + contentId + "/progress")
                .content("{\"progressSeconds\":0}"))
                .andExpect(status().isOk());
        detail = data(call(USER_A, get("/h5/api/contents/" + contentId)));
        assertThat(detail.get("progressSeconds").asInt()).isEqualTo(125);
        assertThat(data(call(USER_A, get("/h5/api/contents/continue-watching")))
                .get("contentId").asText()).isEqualTo(contentId);

        // 进度按用户隔离；看到结尾后不再显示在“正在看”
        assertThat(data(call(USER_A_OTHER, get("/h5/api/contents/continue-watching"))).isNull()).isTrue();
        assertThat(data(call(USER_A_OTHER, get("/h5/api/contents/" + contentId)))
                .get("progressSeconds").asInt()).isZero();
        call(USER_A, put("/h5/api/contents/" + contentId + "/progress")
                .content("{\"progressSeconds\":9999}"))
                .andExpect(status().isOk());
        assertThat(data(call(USER_A, get("/h5/api/contents/continue-watching"))).isNull()).isTrue();

        // 另一个租户完全看不到
        call(USER_B, get("/h5/api/contents/" + contentId)).andExpect(status().isNotFound());
        call(USER_B, get("/h5/api/contents?categoryId=" + cat)).andExpect(status().isNotFound());
        assertThat(data(call(USER_B, get("/h5/api/contents"))).get("total").asLong()).isZero();
        call(ADMIN_B, post("/admin/api/contents/" + contentId + "/unpublish")).andExpect(status().isNotFound());
        call(ADMIN_B, delete("/admin/api/contents/" + contentId)).andExpect(status().isNotFound());

        // 禁用分类后 H5 不可见
        call(ADMIN_A, put("/admin/api/categories/" + cat).content("{\"name\":\"视频课\",\"status\":0}"))
                .andExpect(status().isOk());
        call(USER_A, get("/h5/api/contents/" + contentId)).andExpect(status().isNotFound());
    }

    @Test
    void uploadTasksAreIsolatedAndSingleUse() throws Exception {
        String cat = createCategory(ADMIN_A, "文档", null);
        String taskA = uploadFile(ADMIN_A, "DOC", "a.pdf", 3 * MB);

        // B 不能引用 A 上传的文件，也不能查看 / 操作 A 的上传任务
        String catB = createCategory(ADMIN_B, "B文档", null);
        call(ADMIN_B, post("/admin/api/contents").content("""
                {"categoryId":"%s","type":"DOC","title":"偷","fileTaskId":"%s"}""".formatted(catB, taskA)))
                .andExpect(status().isNotFound());
        call(ADMIN_B, get("/admin/api/uploads/" + taskA)).andExpect(status().isNotFound());
        call(ADMIN_B, post("/admin/api/uploads/" + taskA + "/part-urls").content("{\"partNumbers\":[1]}"))
                .andExpect(status().isNotFound());

        // 类型不匹配
        call(ADMIN_A, post("/admin/api/contents").content("""
                {"categoryId":"%s","type":"VIDEO","title":"x","fileTaskId":"%s"}""".formatted(cat, taskA)))
                .andExpect(status().isBadRequest());
        // 正常引用一次后不可再用
        call(ADMIN_A, post("/admin/api/contents").content("""
                {"categoryId":"%s","type":"DOC","title":"讲义","fileTaskId":"%s"}""".formatted(cat, taskA)))
                .andExpect(status().isOk());
        call(ADMIN_A, post("/admin/api/contents").content("""
                {"categoryId":"%s","type":"DOC","title":"讲义2","fileTaskId":"%s"}""".formatted(cat, taskA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadValidation() throws Exception {
        call(ADMIN_A, post("/admin/api/uploads").content(
                "{\"purpose\":\"VIDEO\",\"fileName\":\"a.mp4\",\"fileSize\":" + (2049L * MB) + "}"))
                .andExpect(status().isBadRequest());
        call(ADMIN_A, post("/admin/api/uploads").content(
                "{\"purpose\":\"VIDEO\",\"fileName\":\"a.exe\",\"fileSize\":100}"))
                .andExpect(status().isBadRequest());

        // 分片不全不能合并；补齐后可以（断点续传）
        JsonNode init = data(call(ADMIN_A, post("/admin/api/uploads").content(
                "{\"purpose\":\"VIDEO\",\"fileName\":\"b.mp4\",\"fileSize\":" + (15 * MB) + "}")));
        String id = init.get("taskId").asText();
        assertThat(init.get("partCount").asInt()).isEqualTo(2);
        String uploadId = uploadIdOf(id);
        storage.putPart(uploadId, 1, 10 * MB);
        call(ADMIN_A, post("/admin/api/uploads/" + id + "/complete")).andExpect(status().isBadRequest());
        assertThat(data(call(ADMIN_A, get("/admin/api/uploads/" + id))).get("uploadedParts").toString())
                .isEqualTo("[1]");
        storage.putPart(uploadId, 2, 5 * MB);
        call(ADMIN_A, post("/admin/api/uploads/" + id + "/complete")).andExpect(status().isOk());
    }

    @Test
    void textContentIsSanitized() throws Exception {
        String cat = createCategory(ADMIN_A, "文章", null);
        String coverTask = uploadFile(ADMIN_A, "COVER", "cover.jpg", 256 * 1024);
        String id = data(call(ADMIN_A, post("/admin/api/contents").content(om.writeValueAsString(java.util.Map.of(
                "categoryId", cat, "type", "TEXT", "title", "文章",
                "coverTaskId", coverTask,
                "body", "<p onclick=\"x()\">你好<script>alert(1)</script><a href=\"javascript:x\">l</a></p>")))))
                .asText();
        assertThat(data(call(ADMIN_A, get("/admin/api/contents/" + id))).get("coverUrl").asText())
                .startsWith("https://obs.test/tenantA/cover/");

        call(ADMIN_A, put("/admin/api/contents/" + id).content(om.writeValueAsString(java.util.Map.of(
                "categoryId", cat, "type", "TEXT", "title", "文章", "removeCover", true))))
                .andExpect(status().isOk());
        assertThat(data(call(ADMIN_A, get("/admin/api/contents/" + id))).get("coverUrl").isNull()).isTrue();

        call(ADMIN_A, post("/admin/api/contents/" + id + "/publish")).andExpect(status().isOk());
        String body = data(call(USER_A, get("/h5/api/contents/" + id))).get("body").asText();
        assertThat(body).contains("你好").doesNotContain("script").doesNotContain("onclick")
                .doesNotContain("javascript");
    }

    // ---------- helpers ----------

    @Autowired com.learning.platform.module.upload.mapper.UploadTaskMapper taskMapper;

    private String uploadIdOf(String taskId) {
        // 绕过租户插件直接读取（测试中没有请求上下文）
        com.learning.platform.auth.TenantContext.set(
                new com.learning.platform.auth.Principal("tenantA", "t", java.util.Set.of()));
        try {
            return taskMapper.selectById(Long.parseLong(taskId)).getUploadId();
        } finally {
            com.learning.platform.auth.TenantContext.clear();
        }
    }

    private String uploadFile(String token, String purpose, String name, long size) throws Exception {
        JsonNode init = data(call(token, post("/admin/api/uploads").content(
                "{\"purpose\":\"%s\",\"fileName\":\"%s\",\"fileSize\":%d}".formatted(purpose, name, size))));
        String id = init.get("taskId").asText();
        long partSize = init.get("partSize").asLong();
        int count = init.get("partCount").asInt();
        String tenant = token.split(":")[1];
        com.learning.platform.auth.TenantContext.set(
                new com.learning.platform.auth.Principal(tenant, "t", java.util.Set.of()));
        String uploadId;
        try {
            uploadId = taskMapper.selectById(Long.parseLong(id)).getUploadId();
        } finally {
            com.learning.platform.auth.TenantContext.clear();
        }
        for (int i = 1; i <= count; i++) {
            storage.putPart(uploadId, i, i < count ? partSize : size - partSize * (count - 1));
        }
        call(token, post("/admin/api/uploads/" + id + "/complete")).andExpect(status().isOk());
        return id;
    }

    private String createCategory(String token, String name, String parent) throws Exception {
        String body = parent == null ? "{\"name\":\"%s\"}".formatted(name)
                : "{\"name\":\"%s\",\"parentId\":\"%s\"}".formatted(name, parent);
        return data(call(token, post("/admin/api/categories").content(body))).asText();
    }

    private ResultActions call(String token, MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON));
    }

    private JsonNode data(ResultActions r) throws Exception {
        String s = r.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8);
        return om.readTree(s).get("data");
    }
}
