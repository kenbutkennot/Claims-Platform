# AI Working Journal

## Entry 1 - Repository inspection and implementation approach
**Prompt / instruction:** Build the claims-management application described in the supplied assessment brief.

**Suggestion:** Use the prescribed Java/Spring Boot and Angular stack, with SQLite, synchronous REST for core work, and a local development seed.

**Decision:** Accepted.

**Reasoning:** The workspace was empty and the brief fixed the backend and database choices. Keeping a single backend module and avoiding Kafka and real authentication limits scope without weakening the core workflow.

**Verification:** Confirmed the workspace had no existing project files. Node 22 and npm were available; Temurin Java 21 was installed before final backend verification.

## Entry 2 - Persistence and claim amounts
**Prompt / instruction:** Implement the domain, persistence, lifecycle rules, and dashboard in line with the brief.

**Suggestion:** Persist money as `BigDecimal` using a JPA converter to decimal text, and calculate exposure in Java.

**Decision:** Accepted.

**Reasoning:** SQLite does not provide exact decimal storage semantics. Text conversion avoids silent floating-point coercion and makes aggregation behavior explicit.

**Verification:** Exact decimal persistence and foreign-key test passed. Dashboard exposure calculation test passed.

## Entry 3 - Demo role and workflow design
**Prompt / instruction:** Support claimant and staff views in one Angular app while making demo-only authentication explicit.

**Suggestion:** Use seeded demo users selected in the UI and pass the selected identity as a request header, with ownership and staff checks centralized in the backend.

**Decision:** Accepted with an explicit demo-only warning.

**Reasoning:** The take-home needs immediately usable roles without pretending a client selector is secure authentication. The server still checks claim ownership and staff/assignee role for workflow operations.

**Verification:** Angular production build passed. Backend workflow tests passed, including authorization-aware service operations and history writes.

## Entry 4 - Runtime verification
**Prompt / instruction:** Install Java 21 if needed and verify the backend and frontend builds and seeded API startup.

**Suggestion:** Explicitly select the installed Temurin 21 JDK for Maven and inspect runtime startup failures rather than hiding them.

**Decision:** Accepted.

**Reasoning:** The machine initially had Java 17 and Maven compilation reported release 21 unsupported. Temurin 21 was installed, and Maven tests pass under Java 21.

**Verification:** Angular production build passed. Backend tests passed 8/8. The first API launch exposed ambiguous `ClaimService` constructors; marking the dependency-injection constructor with `@Autowired` fixed startup. A subsequent launch initialized SQLite, loaded six seeded claims, and returned dashboard and demo-user API responses successfully.
