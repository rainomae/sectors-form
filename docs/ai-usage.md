# AI usage

## Tool and way of working

The implementation was done with Claude Code (Anthropic's Claude in the terminal) as a pair
programmer. The work was split into eight steps, each delivered as one reviewable commit: after
every step the generated code was read, run and tested locally before the next step started.
Direction, review, design decisions and all commits were mine; Claude wrote the code, tests and
documentation from the prompts below, ran the builds and tests, and drove a browser to verify the UI.

Where AI was used:

- **Backend** (Spring Boot): entities, repositories, services, controllers, validation, problem
  details, Flyway migrations and the script that generates the sector seed from `index.html`,
  MockMvc/JPA/unit tests, Gradle build with Spotless and JaCoCo.
- **Frontend** (React + TypeScript + Vite): the form, API client, sector select box and its
  usability layer, styling, Vitest unit tests.
- **End-to-end tests** (Playwright) and **Docker Compose** (Dockerfiles, nginx).
- **Documentation**: this file and the README.
- **Environment troubleshooting**: WSL 2 and Docker Desktop start-up problems on Windows.

## Prompts

### Kick-off

> Read `index.html` and, before writing any code, expand the task into concrete requirements and
> propose a data model. Stack: Java, Spring Boot, Gradle, Spring Data JPA, Flyway, PostgreSQL via
> Docker Compose, H2 in tests, springdoc-openapi, Spotless, JaCoCo. Backend structured by feature,
> not by layer; DTOs are records; entities are never returned from controllers. Frontend: React +
> TypeScript + Vite. E2E: Playwright. MockMvc integration tests. README in English: what was wrong
> with `index.html` and how each item was fixed, how to run it with Docker Compose and locally, and a
> section justifying every significant choice.

> Work step by step and pause after each step so that every step can be reviewed and committed
> separately. Each step must build and its tests must pass before it is handed over.

### Backend

> Structure every package the same way: the feature package holds the entity, a public repository,
> the service, the controller and the exceptions, DTO records live in a `dto` sub-package, and
> cross-cutting classes (exception bases, global exception handler, OpenAPI and cache configuration)
> live in `shared`. Keep annotations minimal: no schema annotations on DTOs, no OpenAPI boilerplate
> beyond a summary and response codes. Everything must be immediately readable.

> Justify why the sector table keeps the original `option value` attributes as primary keys instead
> of generated ids.

> The sector list is reference data; cache it after the first read.

> A separate Bean Validation constraint for unknown sector ids is more machinery than the check
> deserves; replace it with a check in the service that maps to a 400 problem detail.

> Record components should be laid out one per line for readability. Keep Spotless, but use a
> formatter configuration that preserves hand-placed line breaks.

### Frontend

> Keep the native `select multiple` as the task specifies a select box, but make it usable: a plain
> click toggles one sector without dropping the others, the list must not scroll back to the top
> when a row is clicked, a filter field must match partial words anywhere in a sector's path (so a
> parent's name shows its whole group), and there must be a Clear all action.

> Colour-code the rows by top-level sector so the type is visible at a glance, and make the
> sub-sector levels distinguishable from each other. Main sectors must not look like non-clickable
> headers; every row is selectable. The selected state must be clearly visible and readable whether
> or not the list has focus.

> Mark required fields with `*`, show an example value in the Name field ("e.g. John Doe"), and
> show which sectors are already selected. Place the selection summary (count, chips, Clear all)
> together directly under the list, where the user is looking.

> When the list is scrolled, show which group the visible sub-sectors belong to; it must not look
> like a header row, appear only when the group's own row is out of view, and act as a link back to
> that row.

> The design must look professional: a consistent token-based stylesheet (color, radius, shadow,
> type scale), properly aligned controls, custom-styled checkbox and filter field, a fixed-height
> list that does not shrink when a filter has no matches, and an empty-state message inside the
> list. Validation errors must disappear as soon as the field is fixed.

> Small interaction details must be right: the clear button of the filter field shows a pointer
> cursor, the remove icon on a chip is centred, and the "Agree to terms" checkbox toggles only when
> the box itself or its text is clicked, not anywhere on the row.

### Tests, Docker, documentation

> Add Playwright end-to-end tests that cover the sector hierarchy, all validation errors, save,
> reload, edit, session isolation between browsers, filtering and the chips. The suite should start
> the backend and the frontend itself, using an H2 profile so no database container is needed, and
> also be runnable against the Docker Compose stack. Explain what each test proves.

> Provide Docker Compose with PostgreSQL, a backend image built from source and an nginx image
> serving the frontend and proxying `/api`. Verify the stack end to end, including that data is
> written to PostgreSQL.

> Keep the README minimal: what is needed and how to run it, what was wrong with `index.html` and
> how it was fixed, how the tasks were solved, the justification of choices and the
> AI-usage description.
