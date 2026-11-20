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

## Entry 5 - Frontend API proxy
**Prompt / instruction:** Diagnose the app taking time to load after the Spring Boot startup succeeded.

**Suggestion:** Check the browser-rendered state and verify the frontend API path against the backend.

**Decision:** Added an Angular dev-server proxy for `/api` requests to port 8080.

**Reasoning:** The backend was healthy, but Angular used relative `/api` URLs and no proxy, so the app tried to fetch demo users from port 4200 and showed its API-unavailable state. A dev-server proxy is the smallest local-development fix and avoids changing API URL handling in components.

**Verification:** Confirmed the proxy returns seeded users as JSON and verified the browser renders all six seeded claims with the manager queue after role selection. Angular production build and `git diff --check` passed.

## Entry 6 - Claim filters and role navigation
**Prompt / instruction:** Implement working claim search/status/type filters, role-specific navigation on role switches, and resolve claimant submission returning 403.

**Suggestion:** Bind filter control events to API reloads and navigate to a role-specific landing route whenever the selected demo user changes.

**Decision:** Accepted.

**Reasoning:** The native `value` bindings did not update the component fields, so requests kept using empty filters. Role changes previously left the router on its old page; switching while on the claimant form could leave an officer on the form, causing the backend's correct claimant-role check to reject submission. Search/status/type values now update before reloading, and switching claimant/staff always redirects to My Claims/dashboard respectively.

**Verification:** Angular production build passed. Browser checks confirmed search for `0001` returns one result, `UNDER_REVIEW` returns the review claim, and `MOTOR` returns three claims. Role switching from the incident form redirects to `/dashboard` for staff and back to `/claims` for the claimant, with the selected demo user and page heading matching. No test claim was submitted.
