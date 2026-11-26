# Admin application

## Scope

The admin application uses Vue 3, Vite, strict TypeScript, Element Plus, Pinia, Vue Router, Axios, and wangEditor.

Routes:

- `/login`: unified login redirect plus dev-only convenience login.
- `/categories`: two-level category tree table.
- `/contents`: content filters, pagination, publish/unpublish, delete.
- `/contents/new`: create content.
- `/contents/:id/edit`: edit content.

## Authentication

On application startup, a `token` URL parameter is stored in session storage and removed from the URL. Axios adds `Authorization: Bearer ...`. HTTP 401 clears the token and redirects to login.

The local `admin / Admin@123` form is displayed only by Vite in development mode. It creates `dev:t1:u1:admin`; it is not a backend password endpoint.

## Category management

The tree supports create, edit, delete, enable/disable, and sort. The backend limits nesting to two levels and rejects deletion when a category contains children or content.

JSON IDs and `parentId` are strings. Root `parentId` may arrive as string `"0"`; root checks must handle that value explicitly rather than relying on JavaScript truthiness.

## Content management

List filters: category, type, publication status, keyword, page, and size. Create/edit supports VIDEO, DOC, and TEXT. Type cannot change after creation.

- VIDEO/DOC use completed upload task IDs.
- VIDEO duration is read from browser media metadata and can be corrected manually.
- TEXT uses wangEditor and the backend sanitizes saved HTML.
- Cover upload is optional.
- Editing an existing cover offers separate replace and delete actions. Delete sets `removeCover=true` and takes effect only when the content is saved; uploading a replacement cancels pending deletion.
- Existing media remains unchanged when an edit sends no new file task ID.

## Multipart uploader

`ChunkUploader.vue` follows this sequence:

1. Initialize the task.
2. Query current progress on resume.
3. Request signed URLs for pending parts.
4. Run at most three workers concurrently.
5. PUT chunks directly to storage.
6. Complete through the backend.

Pause aborts current browser requests. Resume asks the backend/storage for completed parts. Failed uploads retain the task and retry pending parts.

## Critical frontend invariant: IDs are strings

Backend snowflake IDs exceed `Number.MAX_SAFE_INTEGER`. Content, category, and upload task IDs must stay strings through:

- response types;
- route parameters;
- select option values;
- map keys;
- API path construction;
- emitted uploader events.

Never use `Number(route.params.id)`, `parseInt`, unary `+`, or `number` for these IDs. This previously changed the tail digits and caused edit requests to return “内容不存在”.

## Verification

Run from `admin/`:

```bash
npm run typecheck
npm run build
```

The production build may warn about large wangEditor/Element Plus chunks. That is a performance warning, not a compilation failure.
