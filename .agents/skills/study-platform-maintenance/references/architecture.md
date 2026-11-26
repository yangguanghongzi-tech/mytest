# Architecture and local operation

## Applications and ports

| Application | Directory | Local port | Responsibility |
|---|---|---:|---|
| Backend | `backend/` | 8080 | Authentication filter, tenant isolation, business rules, MySQL, upload orchestration, file URLs |
| Admin | `admin/` | 5173 | Category and content management, multipart upload |
| H5 | `h5/` | 5174 | User login, category navigation, published content viewing |
| MySQL | Homebrew service | 3306 | `learning` business database |

Admin proxies `/admin/api` and `/local-storage` to port 8080. H5 proxies `/h5/api` and `/local-storage` to port 8080.

## Local startup

The project declares Java 17. On this machine, set it explicitly because the default Maven runtime can resolve to Java 27:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
DB_USER=root \
DB_PASSWORD='' \
AUTH_MODE=dev \
OBS_ENABLED=false \
APP_LOCAL_STORAGE_ROOT=/Users/liuxiaohong/work/study/local-storage \
mvn spring-boot:run
```

Start frontends in separate terminals:

```bash
cd admin && npm run dev
cd h5 && npm run dev
```

MySQL is installed as Homebrew `mysql@8.4`. The local database is `learning`, root currently uses an empty password, and Flyway creates or validates the schema during backend startup.

## Authentication and tenancy

`TokenAuthFilter` protects `/admin/api/**` and `/h5/api/**`. It reads `Authorization: Bearer <token>` and populates `TenantContext`. MyBatis-Plus automatically scopes business tables by `tenant_id`.

Development tokens use:

```text
dev:{tenantId}:{userId}[:{roles}]
```

Current local conveniences:

- Admin form `admin / Admin@123` stores `dev:t1:u1:admin` in session storage.
- H5 form `user / User@123` stores `dev:t1:u1` in session storage.
- Both therefore read tenant `t1` data.

These username/password pairs exist only in frontend development builds. They do not authenticate against MySQL or a backend user table. Production uses JWT configuration and a unified login redirect.

## Data ownership

- MySQL stores categories, content metadata, text bodies, media metadata, and upload task state.
- Binary video, document, and cover data is never stored in MySQL.
- With OBS enabled, binary data is in Huawei OBS.
- In dev with OBS disabled, binary data is under `app.local-storage.root`; set `APP_LOCAL_STORAGE_ROOT` to avoid the default temporary directory.

## End-to-end content flow

1. Admin creates enabled first- or second-level categories.
2. Admin initializes an upload task, uploads file parts directly to a signed URL, and asks the backend to complete the multipart upload.
3. Admin creates content referencing completed upload task IDs. New content starts as `DRAFT`.
4. Admin explicitly publishes it, changing status to `PUBLISHED`.
5. H5 returns it only for the same tenant and only when its category hierarchy is visible.
6. Text bodies come from MySQL; video/document URLs are generated from the configured storage service.
7. H5 records video playback progress per tenant and user. Returning to the same video resumes from the saved second, and the home page shows the most recently watched unfinished video.
