# Chubb APAC Claims Workspace

A local assessment prototype for claimant self-service and claims operations. The Angular app uses a single REST API backed by a Spring Boot modular monolith and a SQLite file.

## What is implemented

- Claimant: submit motor/property claims, view their own claims and timeline, and answer open information requests.
- Claims staff: view and filter the queue, assign claims, request information, approve or reject with a reason, and record settlement.
- Dashboard: status counts, per-officer active and assigned workload, estimated outstanding exposure, and approved-but-unsettled amounts.
- Server-side lifecycle, role, and claimant ownership checks; request validation; RFC 7807-style API errors; append-only workflow history.
- Deterministic development seed with one claimant, two officers, one manager, and six fictional claims spanning the lifecycle.
- SQLite persistence with UUIDs stored as text and `BigDecimal` amounts converted to decimal strings. The database does not use floating-point currency values.

This is a demo application, not an insurance production system. Demo role selection is not authentication; there is no real identity provider, document storage, payment integration, notification service, Kafka, or transactional outbox.

## Architecture and decisions

- `backend/`: Java 21, Spring Boot 3.5, Maven Wrapper, Spring MVC, Spring Data JPA, SQLite, Bean Validation, and springdoc-openapi.
- `frontend/`: Angular 20 standalone components, Angular Router, reactive forms, typed API models, and a shared HTTP interceptor.
- Core claim mutations are synchronous REST calls so the user gets an immediate result. Notifications and reporting could be asynchronous in a production version; Kafka was not added only for demonstration. A production event integration should use a transactional outbox.
- Hibernate `ddl-auto=update` is used to keep this time-boxed prototype runnable without a separate migration step. A production system should use versioned migrations and a server database such as PostgreSQL.
- Estimated outstanding exposure is the sum of estimated amounts excluding rejected and settled claims. Approved-but-unsettled amount sums approved amounts for `APPROVED` claims only. These are operational estimates, not paid loss.
- The default development profile seeds sample users and claims on an empty database. Seed data uses fictional identities and `.test` email addresses.

## Prerequisites

- Java 21
- Node.js 20.19+ (Node 22 recommended) and npm
- Windows PowerShell for the commands below

The project targets Java 21. The development environment used during implementation had Java 17 only; the Maven wrapper can be invoked with `-Djava.version=17` for local verification because the source uses Java 17-compatible language features. Run normally with Java 21.

## Run locally

From the project root, start the backend in one terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

In another terminal, start Angular:

```powershell
cd frontend
npm start
```

Open [http://localhost:4200](http://localhost:4200). The API listens on port 8080. Swagger UI is at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

The SQLite file is `backend/claims.db` when the backend is launched from `backend/`. Set `CLAIMS_DB_PATH` to choose another file. Stop the backend and delete `claims.db` (plus optional `claims.db-shm` and `claims.db-wal`) to reset the local data; the next development start recreates the schema and seed.

## Demo roles and workflow

Use the **Demo user** selector in the top bar to switch between Maya Tan (claimant), Rafiq Lim or Anika Rao (officers), and Darren Koh (manager). This selector changes a demo identity header; it is deliberately not represented as secure authentication.

Suggested demonstration:

1. As Maya, open `My claims`, submit a motor or property claim, and open its detail page.
2. Switch to Rafiq or Anika, open the claims queue, assign the new claim to yourself, and request more information.
3. Switch back to Maya and submit a response on the claim detail page.
4. Switch to the assigned officer, approve or reject with a reason; for approved claims, record settlement.
5. View the updated history and team dashboard.

The API uses `X-Demo-User` to identify the selected seeded user. The backend performs the corresponding claimant ownership and staff/assignee checks. Do not deploy this demo header mechanism as production authentication.

## API routes

- `POST /api/claims`, `GET /api/claims`, `GET /api/claims/{id}`
- `PATCH /api/claims/{id}/assign`
- `POST /api/claims/{id}/information-requests`
- `POST /api/claims/{id}/information-requests/{requestId}/response`
- `POST /api/claims/{id}/decision`, `POST /api/claims/{id}/settlement`
- `GET /api/claims/{id}/history`
- `GET /api/dashboard/summary`, `GET /api/dashboard/workload`
- `GET /api/demo/users`

Claim lists accept optional `status`, `type`, `assignedOfficerId`, `claimantId`, and `search` query parameters. The prototype returns the filtered list without pagination.

## Tests

```powershell
cd backend
.\mvnw.cmd test
```

On a Java 17-only machine, use `.\mvnw.cmd -Djava.version=17 test` for compatibility verification. Frontend production build:

```powershell
cd frontend
npm run build
```

The backend tests cover lifecycle transitions, submission and validation, information requests and responses, decision/settlement rules, audit records, exact decimal persistence, and exposure calculations.

## Known limitations

- Demo role switching is not authentication and no API user token or session is issued.
- Document uploads and storage, email/SMS, Kafka, outbox publishing, payment processing, advanced permissions, and multi-market rules are deferred.
- Claim queue filtering is in-memory and unpaginated for the small seeded dataset.
- SQLite uses `ddl-auto=update`; no schema migration history is included.
- Frontend and API currently support the local assessment workflow only.

## Candidate review checklist

- Be ready to explain the status transition rules in `Claim`, and why workflow operations and history writes are transactional.
- Review `DemoActorService` and the `X-Demo-User` interceptor carefully. They provide demo-only authorization, not production authentication.
- Understand `ExactDecimalConverter` and the exposure definitions in `DashboardService`.
- Be prepared to explain why the user-facing workflow uses REST and where a transactional outbox would fit if asynchronous notification events were introduced.
