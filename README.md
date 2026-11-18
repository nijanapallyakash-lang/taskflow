# TaskFlow: Enterprise Project & Issue Tracking System

Full-stack project management and issue tracker: projects, sprints, Kanban board, tasks with sub-tasks/checklists/attachments, discussion threads, time tracking, burndown dashboards, audit logs, email notifications and CSV/PDF reports.

**Stack:** Java 17 · Spring Boot 3.3 (MVC, Data JPA/Hibernate, Security + JWT, Mail) · PostgreSQL 16 + Flyway · React 18 + Vite + Tailwind CSS · Docker · Render

## Quick start (Docker)

```bash
docker compose up -d --build
```

| URL | What |
|---|---|
| http://localhost:3000 | Web app |
| http://localhost:8080/swagger-ui.html | Swagger / OpenAPI docs |
| http://localhost:8025 | Mailpit, which catches all outgoing email (verification links, notifications) |

Default admin: `admin@taskflow.local` / `Admin@12345` (override with `ADMIN_EMAIL` / `ADMIN_PASSWORD`).

## Local development

```bash
docker compose up -d db mail                       # Postgres + Mailpit
cd backend && mvn spring-boot:run                  # API on :8080 (Flyway migrates on start)
cd frontend && npm install && npm run dev          # UI on :5173, proxies /api to :8080
```

## Tests

```bash
cd backend && mvn test
```
- `TaskServiceTest`, `AuthServiceTest`: JUnit 5 + Mockito service-layer tests
- `AuthControllerTest`: Spring MVC Test (`@WebMvcTest`) for validation and the uniform error JSON
- `postman/`: collection + environment with automated assertions (`newman run postman/TaskFlow.postman_collection.json -e postman/TaskFlow.local.postman_environment.json`)

## Roles

| Action | Admin | Project Manager | Team Member |
|---|:-:|:-:|:-:|
| Create projects | ✓ | ✓ | |
| Edit/archive project, manage members, plan/start/complete sprints | ✓ | ✓ (own projects) | |
| Create/edit tasks, comment, log work, attach files | ✓ | ✓ | ✓ (member projects) |
| Delete task | ✓ | ✓ | reporter only |
| User management (activate/deactivate, roles) | ✓ | | |

## Configuration (env vars)

| Var | Default |
|---|---|
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` | `localhost` `5432` `taskflow` `taskflow` `taskflow` |
| `JWT_SECRET` (≥ 32 chars) | dev value, **change in prod** |
| `JWT_EXPIRATION_MINUTES` | `1440` |
| `FRONTEND_URL` (CORS + email links) | `http://localhost:5173` |
| `MAIL_HOST` `MAIL_PORT` `MAIL_USERNAME` `MAIL_PASSWORD` `MAIL_SMTP_AUTH` `MAIL_STARTTLS` | Mailpit on `localhost:1025` |
| `UPLOAD_DIR` | `uploads` |

## Deploy to Render

`render.yaml` is a Blueprint: in Render, choose **New → Blueprint** and select this repo. Fill in the `sync: false` vars (SMTP, admin, `FRONTEND_URL`). Render's disk is ephemeral, so uploaded attachments and avatars are lost on redeploy unless you attach a persistent disk at `UPLOAD_DIR`.

## API overview

`/api/auth` (register, verify, login) · `/api/users/me` (profile, avatar, password) · `/api/admin/users` · `/api/projects` (CRUD, archive, members, sprints, tasks, activity, dashboard, export) · `/api/sprints/{id}` (start/complete) · `/api/tasks/{id}` (detail, status, sub-tasks, checklist, comments, worklogs, attachments). Full spec at `/v3/api-docs`.
