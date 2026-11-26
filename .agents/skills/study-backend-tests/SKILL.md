---
name: study-backend-tests
description: Run, diagnose, fix, and re-run the Study project's Java backend tests. Use when a request asks to verify backend tests, fix failing backend tests, or confirm the backend build health.
---

# Study Backend Tests

The backend is the Maven project in `backend/` and requires Java 17. On the current development machine, Maven otherwise selects Java 27, which is incompatible with the project's Lombok version during a clean compile. Prefix Maven commands with:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH
```

For system behavior, API contracts, and the verified issue history beyond test execution, read the sibling `study-platform-maintenance` skill and only its relevant references.

## Workflow

1. Run the complete backend test suite from `backend/` with `mvn test` before making code changes.
2. Wait for Maven's final `Results` and `BUILD SUCCESS` or `BUILD FAILURE` lines. Do not treat an old Surefire report or partial console output as the current result.
3. If tests fail, inspect the current Surefire reports under `backend/target/surefire-reports/`, identify the code or test defect, and make the smallest relevant fix. Preserve unrelated working-tree changes.
4. Re-run the affected test while iterating when useful, then run the complete `mvn test` suite again. Completion requires the full suite to pass.
5. Report the command, total tests, failures, errors, skipped tests, and final build status. Summarize changed files when a fix was needed.

If Maven cannot access the user's local repository under `~/.m2`, recognize that as an execution-permission or dependency-resolution issue rather than a test failure. Request the minimum required permission and retry the same test command.

Warnings about optional remote metadata, newer JDK reflective access, or Flyway's tested H2 range do not count as failures when Maven exits successfully and reports `BUILD SUCCESS`; mention them only if they affect the requested outcome.
