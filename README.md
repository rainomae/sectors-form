# Sectors form

The static `index.html` fragment turned into a working application: a person enters their name,
picks sectors from a hierarchical list, agrees to the terms and saves. The data is stored in
PostgreSQL, refilled into the form, and stays editable by the same person during their browser
session.

Backend: Java 25, Spring Boot 4.1, Gradle, Spring Data JPA, Flyway, PostgreSQL (H2 in tests),
springdoc-openapi, Spotless, JaCoCo. Frontend: React 19, TypeScript, Vite. Tests: JUnit 5 + MockMvc,
Vitest, Playwright. Runtime: Docker Compose.

```
backend/    Spring Boot API (ee.sectorsform: sector, submission, shared)
frontend/   React app, e2e/ Playwright tests
docs/       database-dump.sql (structure and data), ai-usage.md, original/index.html
```

## Running

You need Docker with Compose (on Windows: Docker Desktop with WSL 2), JDK 25 and Node.js 22+.
All commands run from the repository root; in PowerShell use `.\gradlew` instead of `./gradlew`.

Everything in Docker (first build takes a few minutes):

```bash
docker compose up --build
```

Form: `http://localhost:3000`. API documentation: `http://localhost:8080/swagger-ui.html`.
Stop with `docker compose down` (`-v` also deletes the data).

For development, run the parts on the host:

```bash
docker compose up -d db                 # PostgreSQL on localhost:5432 (sectors / sectors / sectors)
cd backend && ./gradlew bootRun         # API on http://localhost:8080
cd frontend && npm install && npm run dev   # form on http://localhost:5173, /api proxied to 8080
```

Without Docker, the backend can run on in-memory H2: `cd backend && ./gradlew bootRun --args=--spring.profiles.active=h2`.

Tests:

```bash
cd backend && ./gradlew build           # unit, slice, repository and integration tests on H2; JaCoCo gates: 90 % lines, 80 % branches
cd frontend && npm test                 # Vitest unit tests
cd frontend && npx playwright install chromium && npm run e2e   # end-to-end; starts backend (h2) and Vite itself
```

`E2E_BASE_URL=http://localhost:3000 npm run e2e` (PowerShell: `$env:E2E_BASE_URL='http://localhost:3000'; npm run e2e`)
runs the end-to-end suite against the Compose stack.

## Task 1: deficiencies in index.html

| Deficiency | Fix |
|---|---|
| Not an HTML document: no doctype, `lang`, charset, title, `body` | Proper HTML5 document (UTF-8, `lang="en"`, title, viewport) |
| No `form`; the Save button submits nothing | A form whose submit handler validates and calls the API |
| Inputs without `id`/`name`; label texts not associated; checkbox unlabelled | Every control has an `id`, `name` and `label`; hints and errors linked with `aria-describedby`; required fields marked with `*` |
| Nothing mandatory, no validation | Client- and server-side validation, all fields required, name ≤ 255 characters |
| 79 sectors hardcoded in markup | Sectors seeded into the database and loaded from the API |
| Hierarchy faked with `&nbsp;` | Real parent/child data; rows indented per level, colour-coded by top-level sector, weight per level |
| `size="5"`, no way to find a sector | Fixed-height list of 12 rows plus a filter that matches partial words anywhere in a sector's path |
| Multi-select drops the selection on a plain click; Ctrl-click undiscoverable | A click toggles one row; the list keeps its scroll position; selected rows highlighted; summary with count, chips (full path) and Clear all under the list |
| Dirty labels: trailing spaces, undecoded `&amp;`, three ambiguous "Other" | Names trimmed and decoded in the seed; chips show the full path |
| Layout via `<br>` | Semantic structure and a token-based stylesheet |
| No persistence, feedback or editing | REST backend, success/error messages, edit mode bound to the session |

## Tasks 2 and 3: how they are solved

- **2.1** `V2__seed_sectors.sql` is generated from `docs/original/index.html` by
  `backend/tools/generate-sector-seed.mjs`: option value → `id`, `&nbsp;` depth → `parent_id`,
  position → `sort_order`. 79 sectors, 4 levels.
- **2.2** `GET /api/sectors` returns the tree (cached after the first read); the frontend renders it
  into the native `select multiple`.
- **3.1** Client and server apply the same rules; both trim surrounding whitespace (the same Unicode set)
  before checking the name. Validation failures are `400`
  RFC 9457 problem details with an `errors` list of `{field, message}`, shown next to the fields; a
  hand-made request with unknown sector ids gets a `400` whose `detail` names them.
- **3.2** `POST /api/submissions` stores name, sectors (`submission_sector`) and agreement in
  PostgreSQL and binds the new id to the HTTP session.
- **3.3** After a save the form is refilled by reading the stored submission back with
  `GET /api/submissions/current`; the same call prefills the form on page load.
- **3.4** `PUT /api/submissions/{id}` and `GET /api/submissions/{id}` are allowed only for the id
  stored in the session (`403` otherwise). Session cookie: `HttpOnly`, `SameSite=Lax`, 30 minutes.

Data model: `sector` (`id` = original option value, `name`, `parent_id`, `sort_order`),
`submission` (`id`, `name`, `agreed_to_terms`, timestamps), `submission_sector` (many-to-many).
A full dump of the database (structure and data, `pg_dump`) is in `docs/database-dump.sql`.

## Choices and why

- **Original option values as sector primary keys**: they are the taxonomy's existing identifiers;
  keeping them preserves compatibility and traceability and makes the seed deterministic.
- **Flyway owns the schema, Hibernate validates it**: migrations are reviewable SQL; drift between
  entities and tables fails at startup. The seed is generated, not typed.
- **Package by feature, DTOs as records, entities never returned**: everything about a feature in
  one folder; the API contract is independent of persistence details.
- **Ownership in the HTTP session**: the task asks for editing one's own data during the session;
  the standard servlet session does exactly that without token handling. Limitation: sessions are
  in memory, so a backend restart ends them (Spring Session JDBC would fix that).
- **Problem details for all errors**: one handler, one error format, field errors the UI can place.
- **Native select box kept**: the task specifies a select box, and the native control keeps
  keyboard and screen-reader support; its usability problems are solved around it (click-to-toggle,
  filter, chips, breadcrumb, colour coding) instead of by replacing it. Options can only carry text
  and a few CSS properties, so the selected state is a tint plus a bar on the left edge.
- **Validation on both sides**: immediate feedback in the browser, authority on the server.
- **One origin**: Vite in development and nginx in Docker proxy `/api`, so no CORS and a plain
  session cookie.
- **H2 in tests and for the `h2` profile only**: same migrations in milliseconds; excluded from the
  packaged jar.
- **Docker images built from source in two stages**, non-root runtime, health checks; PostgreSQL 17.

## AI usage

Claude Code was used as a pair programmer for the whole implementation under step-by-step review;
`docs/ai-usage.md` describes where it was used and lists the prompts.
