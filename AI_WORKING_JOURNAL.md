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

## Entry 7 - CORS rejection during claim submission
**Prompt / instruction:** Diagnose and fix the repeated 403 when a claimant submits a new incident.

**Suggestion:** Capture the browser request and response, compare the frontend-proxied request with a direct API request, then add a focused CORS regression test.

**Decision:** Accepted.

**Reasoning:** The browser sent the correct seeded claimant UUID. The 403 body was `Invalid CORS request`: `WebConfig` allowed only `http://localhost:4200`, while the shared browser used `http://127.0.0.1:4201`. This was a CORS-origin rejection, not a claimant authorization failure.

**Verification:** Added exact allowlist entries for `localhost` and `127.0.0.1` on Angular ports 4200 and 4201, plus a MockMvc OPTIONS test that verifies these origins can POST with the demo header. Backend tests passed 9/9. The already-running backend process still has the old CORS config; restart it before retrying claim submission.

## Entry 8 - Include managers as claim assignees
**Prompt / instruction:** Include managers among staff who can be assigned to any claim, while excluding claimants from the assignment choices.

**Suggestion:** Provide a staff-protected assignee endpoint listing officers and managers, and enforce the same allowed roles on assignment.

**Decision:** Accepted for all claims; no high-profile claim marker is needed.

**Reasoning:** The general demo-user list includes claimants and is intended for role switching, not assignment. The dedicated `/api/demo/assignees` endpoint and backend assignment check now both allow only officers and managers; the dropdown labels each assignee's role.

**Verification:** Added service regression tests for assignee role filtering, manager assignment, and claimant rejection. Backend tests passed 11/11, Angular production build passed, and `git diff --check` passed. The browser's currently running backend returns 404 for `/api/demo/assignees`, confirming it must be restarted to load this route before the dropdown can populate.

## Entry 9 - Permit empty information responses in the demo
**Prompt / instruction:** Since file uploads are not implemented, allow claimants to submit an empty response to information requests for demo purposes.

**Suggestion:** Accept an empty response string while continuing to reject a missing response value, and make the empty-submit behavior clear in the claimant UI.

**Decision:** Accepted.

**Reasoning:** The API previously rejected empty responses through `@NotBlank`, and the frontend also blocked submission when the text field was blank. The demo now accepts empty text, transitions the request and claim normally, and confirms that no additional response was provided.

**Verification:** Added validation and workflow regression tests for empty and null responses. Backend tests passed 13/13 and `git diff --check` passed. The frontend build was not rerun.
