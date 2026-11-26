# Troubleshooting and verified issue log

Use this file before diagnosing a repeated symptom. Verify current evidence; do not assume every similar symptom has the historical cause.

## Fast diagnosis order

1. Check listeners on 8080, 5173, and 5174 and the MySQL service.
2. Confirm the backend uses Java 17 and reached `Started LearningPlatformApplication`.
3. Call the relevant API with the correct dev token.
4. Inspect MySQL only when API and application state disagree.
5. Check tenant, publication status, category visibility, logical deletion, and exact string ID.
6. After fixes, run the relevant complete test/build commands.

## Issue log

### 2026-11-18 — Backend test result was unclear

- Symptom: an old Surefire report existed, but no current command result was known.
- Evidence: live `mvn test` produced 8 tests, 0 failures/errors/skips and `BUILD SUCCESS`.
- Invariant: use the current final Maven summary, never an old report or partial log.

### 2026-11-18 — Maven could not open `~/.m2` lock files

- Symptom: Maven failed before test execution with `Could not open file channel` under `.m2/repository/.locks`.
- Cause: sandbox permission, not a test failure.
- Fix: request the minimum permission for Maven to access the local repository, then retry the same command.

### 2026-11-19 — MySQL service registration failed

- Symptom: `brew services start mysql@8.4` returned `Bootstrap failed: 5`.
- Evidence: the real launchd label `sh.brew.mysql@8.4` was already registered but not running.
- Fix: `launchctl kickstart -k gui/501/sh.brew.mysql@8.4`; database `learning` was then created with utf8mb4/utf8mb4_unicode_ci.
- Invariant: inspect the actual plist label before deleting service files or database data.

### 2026-11-19 — `OBS_ENABLED=false` prevented backend startup

- Symptom: `UploadService` required a missing `StorageService` bean.
- Cause: OBS implementation was conditional, but no disabled-OBS implementation existed.
- Fix: add `LocalStorageService` and `LocalStorageController`, limited to dev plus OBS disabled; add frontend `/local-storage` proxies.
- Invariant: local development storage is a complete implementation, not a null dependency.

### 2026-11-19 — Clean compile failed with missing Lombok-generated methods

- Symptom: many missing getters, setters, constructors, and log fields after a source change.
- Cause: Maven used Java 27 while the project declares Java 17 and carries Lombok 1.18.34. Cached classes had hidden the problem earlier.
- Fix: run Maven with `/opt/homebrew/opt/openjdk@17` explicitly.
- Invariant: compile and run this project under Java 17; do not diagnose the generated-method avalanche as individual source defects.

### 2026-11-19 — H5 showed no published video

- Evidence: MySQL showed `健康讲座` as `PUBLISHED`, tenant `t1`, category enabled, and not deleted.
- Cause: Vant List set `loading=true` before `load`; the handler returned early when it saw loading true, so no content request ran.
- Fix: add an independent `requesting` guard and set `immediate-check=false` while retaining explicit initial load.
- Invariant: do not use Vant's UI loading model as the request-lock condition.

### 2026-11-19 — Admin edit returned “内容不存在”

- Symptom: list showed content, but edit detail returned 404 and the blank edit form appeared.
- Cause: `Number(route.params.id)` rounded snowflake ID `2123311445708320769` to a different value.
- Fix: convert content, category, and upload task IDs to strings throughout admin.
- Invariant: no JavaScript numeric coercion for backend IDs.

### 2026-11-19 — Videos without explicit covers showed placeholders

- Requirement: use the video's first frame when admin did not upload a cover.
- Fix: add `videoUrl` to backend list items and let H5 load a muted metadata video, seeking near zero; retain explicit cover priority and a fallback.
- Invariant: restart the backend after response DTO changes, then rebuild/refresh H5.

### 2026-11-19 — Video limit increased

- Change: default maximum video size moved from 500 MB to 2 GB in YAML and `UploadProperties`.
- Regression coverage: an upload declaration of 2049 MB is rejected.

### 2026-11-19 — H5 document library presentation

- Requirement: show documents as a dedicated resource-library list styled by file format.
- Fix: add `fileName`, `mime`, and `fileSize` to list responses and render DOC content through `DocumentCard.vue` with PDF/DOC/PPT/XLS colors and actions.
- Invariant: display only real stored metadata; do not fabricate page or popularity statistics from a visual reference.

### 2026-11-20 — Document formats shared a generic icon

- Symptom: a PDF could appear with the same generic document glyph as Word.
- Cause: format colors and labels differed, but PDF/DOC/XLS reused Vant `description-o`.
- Fix: classify the actual file by the final suffix of backend `fileName` and render distinct inline SVGs for PDF, DOC/DOCX, PPT/PPTX, and XLS/XLSX.
- Invariant: backend business type `DOC` is coarse and includes multiple formats. H5 file icons use the filename suffix only; unknown formats use a neutral FILE fallback.

### 2026-11-20 — Verify document filename before changing suffix parsing

- Evidence: MySQL stores `管理系统板块优化（整理版）_20261117170933.pdf`, so its final suffix is `pdf`.
- Observed environment: port 8080 was stopped while checking the live H5 list, so no current response could be inspected.
- Diagnostic: in development, `DocumentCard.vue` logs `fileName`, parsed `fileExtension`, and selected `documentKind` when the card mounts.
- Invariant: after adding list DTO fields such as `fileName`, restart the backend and inspect the live value; an old or stopped backend cannot supply the new field.

### 2026-11-20 — Admin cover removal needed explicit API semantics

- Symptom: hiding the existing cover preview and saving did not remove the database cover.
- Cause: `coverTaskId=null` intentionally meant “do not replace”, so it could not also mean deletion; default ORM null strategy would also skip a null column update.
- Fix: add `removeCover=true`, reject simultaneous replacement/deletion, and allow `cover_key` to be explicitly updated to null. Admin applies deletion only on save.
- Invariant: preserve, replace, and delete are three distinct cover operations and must not be inferred from one nullable task ID.

### 2026-11-20 — Video cover priority on H5

- Requirement: use the uploaded admin cover when present; otherwise show the video's first frame.
- Fix: list cards keep `coverUrl` ahead of the first-frame video fallback, and the detail player uses `coverUrl` as its poster or seeks near time zero when no poster exists.
- Invariant: an explicit cover always wins, while first-frame extraction is only a no-cover fallback.

### 2026-11-20 — Uploaded video cover appeared blank on H5

- Evidence: MySQL contained the cover key, the H5 API returned `coverUrl`, and the file endpoint returned 200, but the browser image had an empty `src` and zero natural dimensions.
- Cause: the list enabled Vant Image `lazy-load` without installing the Vant Lazyload plugin. The local file endpoint also returned a generic binary MIME type.
- Fix: render the explicit cover normally and return extension-derived MIME types from local storage.
- Invariant: only enable Vant image lazy loading after registering its plugin; verify a cover through database, API, HTTP response, and the rendered image element.

### 2026-11-20 — Per-user video resume and continue watching

- Requirement: show the most recently watched unfinished video on the H5 home page and resume at the user's last card point.
- Implementation: Flyway creates `watch_progress`; H5 periodically saves whole playback seconds and detail responses return `progressSeconds`; the latest endpoint filters out completed or no-longer-visible videos.
- Verification: backend tests cover save/read, duration clamping, same-tenant user isolation, and completed-video exclusion.
- Invariant: viewing progress belongs to tenant + user + video, not to the shared content row; backend persistence is the source of truth.

### 2026-11-20 — IDE backend startup failed because port 8080 was occupied

- Symptom: `spring-boot:run` ended with `BUILD FAILURE` and advised stopping the process listening on port 8080.
- Evidence: PID 59492 was the Study backend previously started for API integration testing under Java 17.
- Fix: stop the earlier integration-test process and confirm that no listener remains on 8080 before restarting from the IDE.
- Invariant: only run one backend instance on port 8080; after agent-led live verification, stop its process before handing startup control back to the IDE.

### 2026-11-20 — Refresh cleared video card point and continue-watching card

- Symptom: after watching about 10 seconds, opening the card started from zero; refreshing then removed the home continue-watching module.
- Evidence: `watch_progress.progress_seconds` had been overwritten with 0, so detail returned zero and the recent endpoint returned `null`.
- Cause: `pause` or component teardown could run before video metadata/resume seeking finished and force-save the player's temporary `currentTime=0`.
- Fix: H5 enables writes only after metadata and resume initialization; `ended` alone marks completion; backend ignores a zero report when a positive card point already exists.
- Verification: save 10 seconds, submit a simulated zero, refresh detail and recent endpoints, and confirm all still return 10 seconds; full backend and H5 builds pass.
- Invariant: zero during player initialization is not completion. An unfinished positive card point persists until the player reaches the real duration.

### 2026-11-20 — Video appeared to refresh and jump to zero

- Evidence: local video Range requests correctly returned `206 Partial Content`, browser logs showed no media error, and MySQL retained a positive progress value. The reset was therefore a client media-element reload/race rather than unsupported large-file seeking or a database reset.
- Cause: Vite HMR, page refresh, or browser media recovery can reload the `<video>` element. The prior code only restored the progress captured by the initial detail request and had no latest-second fallback during the unload race.
- Fix: cache the latest positive second in session storage, block progress writes during media `loadstart` / `emptied`, re-seek after metadata, and send the last known second with `keepalive` on `pagehide`.
- Invariant: media element lifecycle is separate from Vue route lifecycle. Resume protection must cover internal media reloads and page-unload races without treating temporary zero as progress.

## Updating this log

Add an entry only after the cause is supported by logs, API output, database state, or a reproducible code path. Include symptom, cause, fix, and reusable invariant. Avoid passwords, access keys, full tokens from real environments, and transient raw logs.
