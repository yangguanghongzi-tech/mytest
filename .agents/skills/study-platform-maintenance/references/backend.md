# Backend behavior and contracts

## Modules

- `auth`: dev/JWT token resolution, admin role enforcement, tenant context.
- `category`: two-level category tree and visibility.
- `content`: VIDEO, DOC, and TEXT lifecycle.
- `upload`: multipart upload task lifecycle.
- `storage`: Huawei OBS production implementation and local dev implementation.
- `common`: response envelope, pagination, errors, base entity.

## API envelope

All controllers return `Result<T>`:

```json
{"code": 0, "message": "ok", "data": {}}
```

HTTP status remains meaningful for 400, 401, 403, 404, 409, and 500. Snowflake `Long` wrapper values are serialized as JSON strings by `WebConfig`; primitive `long` pagination counts remain numeric.

## Admin APIs

- `GET /admin/api/categories`: category tree.
- `POST /admin/api/categories`: create category.
- `PUT /admin/api/categories/{id}`: edit category, status, or order.
- `DELETE /admin/api/categories/{id}`: delete if it has no child or content.
- `GET /admin/api/contents`: filter by category, type, status, keyword; paginate.
- `GET /admin/api/contents/{id}`: edit detail.
- `POST /admin/api/contents`, `PUT /admin/api/contents/{id}`: create or update.
- `POST /admin/api/contents/{id}/publish`, `/unpublish`: lifecycle transition.
- `DELETE /admin/api/contents/{id}`: logical deletion.
- `POST /admin/api/uploads`: initialize multipart upload.
- `POST /admin/api/uploads/{id}/part-urls`: sign part URLs.
- `GET /admin/api/uploads/{id}`: uploaded part numbers for resume.
- `POST /admin/api/uploads/{id}/complete`: validate and merge.
- `DELETE /admin/api/uploads/{id}`: abort.

## H5 APIs and visibility

- `GET /h5/api/categories`: only enabled roots and enabled children under enabled roots.
- `GET /h5/api/contents`: only `PUBLISHED` content whose category is visible; a root category query includes visible children.
- `GET /h5/api/contents/{id}`: same published/category visibility checks.
- `PUT /h5/api/contents/{id}/progress`: save the current user's video playback second; values are clamped to the stored duration.
- `GET /h5/api/contents/continue-watching`: return the current user's most recently watched unfinished visible video, or `null`.

Content created in admin starts as `DRAFT`. Upload completion and content saving do not publish it.

For content saves, `coverTaskId` replaces a cover, omission preserves it, and `removeCover=true` explicitly clears it. Supplying both replacement and deletion is rejected.

`ListItem` includes `videoUrl` for video records. H5 uses this only when `coverUrl` is absent to load a first-frame fallback. It also includes `fileName`, `mime`, and `fileSize` for VIDEO/DOC records so list UIs can show real file metadata without fetching each detail.

Video `Detail` includes `progressSeconds` for the current user. Progress is not content metadata: it is stored per tenant, user, and content in `watch_progress`. Never share progress merely because two users belong to the same tenant. A zero-second initialization report cannot overwrite an existing positive card point. Completed progress (at or beyond duration) remains stored but is excluded from the continue-watching response.

## Tables

| Table | Meaning |
|---|---|
| `category` | Two-level categories, status, sort |
| `content` | Type, title, description, category, cover key, publication state |
| `media_file` | Video/document object key, name, MIME, size, duration |
| `text_body` | Sanitized TEXT HTML |
| `upload_task` | Multipart task, purpose, object key, sizes, lifecycle status |
| `watch_progress` | Per-user video playback second and most-recent watch time |
| `flyway_schema_history` | Applied schema migration history |

All business tables contain `tenant_id` and logical deletion fields. Do not bypass tenant-aware mappers for normal business queries.

## Upload and storage

Upload purposes constrain extensions:

- VIDEO: MP4.
- DOC: PDF, Word, PowerPoint, Excel formats listed in `UploadPurpose`.
- COVER: JPG, JPEG, PNG, WebP.

Defaults: part size 10 MB, video 2 GB, document 100 MB, cover 5 MB. The browser uploads up to three parts concurrently.

`ObsStorageService` is active when `app.obs.enabled=true`. It requires endpoint, bucket, access key, and secret key.

`LocalStorageService` and `LocalStorageController` are active only under the `dev` profile with `app.obs.enabled=false`. They persist parts and objects on disk, but active upload ID-to-object mappings are process-local. A backend restart during an unfinished local upload requires selecting the file again.

Local object responses derive `Content-Type` from the stored filename extension (for example, `image/png` for a PNG cover) and fall back to `application/octet-stream` only for unknown formats.

## Verification

Use the `study-backend-tests` skill. On this machine prefix Maven with Java 17. Current suite is `TenantIsolationTest`, covering authentication, tenant isolation, category rules, upload validation/resume, content publication, and H5 visibility.
