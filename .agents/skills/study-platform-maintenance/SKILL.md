---
name: study-platform-maintenance
description: Maintain, diagnose, extend, or summarize the Study learning platform across its Spring Boot backend, Vue admin, and Vue/Vant H5 applications. Use for project behavior, startup, API, database, upload, authentication, cross-app integration, or previously fixed issue questions.
---

# Study Platform Maintenance

Treat the repository as three cooperating applications:

- `backend/`: Spring Boot 3.3.5, Java 17, MySQL, MyBatis-Plus, Flyway.
- `admin/`: Vue 3, Vite, TypeScript, Element Plus, Pinia, wangEditor.
- `h5/`: Vue 3, Vite, TypeScript, Vant.

Read only the references relevant to the current task:

- Read [architecture.md](references/architecture.md) for startup, ports, data ownership, authentication, tenancy, and end-to-end flows.
- Read [backend.md](references/backend.md) for backend APIs, database tables, visibility rules, upload/storage behavior, and backend changes.
- Read [admin.md](references/admin.md) for management UI behavior, frontend contracts, chunk upload, and admin changes.
- Read [h5.md](references/h5.md) for H5 login, categories, content display, video cover fallback, and H5 changes.
- Read [troubleshooting.md](references/troubleshooting.md) before diagnosing a failure or summarizing project history. It records observed failures and verified fixes.

For backend test execution, also follow the sibling `study-backend-tests` skill. Use Java 17 explicitly on this machine; the default Maven runtime may be Java 27 and is not compatible with the project's Lombok version during a clean compile.

## Maintenance rules

- Preserve Java snowflake IDs as strings in both frontends. Never coerce route, content, category, or upload task IDs with `Number`, `parseInt`, or numeric TypeScript types.
- Keep dev authentication distinct from production authentication. Local username/password forms create dev tokens; they are not a backend password system.
- Keep production OBS and local development storage behavior separate. Local storage must remain limited to the `dev` profile with `app.obs.enabled=false`.
- A content item is visible in H5 only when it is published, belongs to the requesting tenant, and its category hierarchy is enabled.
- After changing a backend response, verify all consumers in `admin/` and `h5/`. Restart the backend when a Java API shape changes.
- After a material feature, decision, or verified bug fix, update the relevant reference and add a concise entry to the issue log in [troubleshooting.md](references/troubleshooting.md). Record evidence and the invariant learned, not raw chat history.

## Verification

- Backend: run the complete Maven suite under Java 17 and require the final Surefire summary plus `BUILD SUCCESS`.
- Admin: run `npm run typecheck` and `npm run build` from `admin/`.
- H5: run `npm run typecheck` and `npm run build` from `h5/`.
- For cross-app changes, verify the relevant API response and the consuming UI rather than relying on compilation alone.
