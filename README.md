# DevOps learning API

Spring Boot backend for one personal learner. Lessons, labs and quizzes are versioned in Git; MariaDB stores only progress, bookmarks, notes, lab checks and quiz attempts.

## Local run

1. Copy `.env.example` to `.env` and replace every password.
2. Start MariaDB: `docker compose --env-file .env up -d`.
3. Run `./run-local.ps1` in PowerShell (or configure the same variables in your IDE, then run `./mvnw.cmd spring-boot:run`).

The app applies Flyway migrations itself. It deliberately refuses to create a public account: the only account is bootstrapped from `APP_BOOTSTRAP_USERNAME` and `APP_BOOTSTRAP_PASSWORD` on its first start.

## API

All routes start with `/api/v1`. Obtain a CSRF token from `GET /api/v1/auth/csrf`, send it in `X-CSRF-TOKEN` for every mutation, and authenticate through `POST /api/v1/auth/login`. Mutations require an `If-Match` value returned by `GET /api/v1/me/study-state`, preventing an old browser tab from silently overwriting newer state.

`GET /api/v1/catalog` strips quiz answers before returning the Git-authored catalog.
