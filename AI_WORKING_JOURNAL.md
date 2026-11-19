# AI Working Journal

## 2026-11-19

### Setup

- Read README, architecture, and compose files.
- Checked Java, Maven, Node, npm, Docker, and Bash prerequisites.
- Started Docker Desktop and infrastructure.
- Installed Maven 3.9.9 and Git Bash.
- Used the repository's local Maven profile to accommodate Windows generation issues.
- Copied the workspace outside Windows Controlled Folder Access and initialized Git there.
- Created the baseline commit before test changes.

### Accepted changes

- Added `ApplicationEventPublisher` mock dependency to `UpdateClaimStatusUseCaseTest`.
- Added event publisher dependencies to the CreateClaim test fixture.
- Replaced a mocked Kafka `ObjectMapper` with a real one in publisher tests.
- Changed theme persistence from `sessionStorage` to `localStorage` after confirming the test contract.
- Added target and node_modules exclusions to `.gitignore`.

### Challenged

- Did not immediately modify production Kafka code because the remaining failures appeared to be stale test stubbing/setup rather than confirmed runtime defects.
- Did not disable or skip failing suites to obtain a green report.
- Did not treat npm audit output as an application defect without assessing whether dependency vulnerabilities are reachable.

### Remaining assessment

- Verify remaining Claims test fixture setup and domain-event expectations.
- Fix remaining wizard test failures based on actual validation timing and accessibility behavior.
- Add a committed test execution script or package script for repeatable local runs.

### Playwright verification

- Added Chromium E2E coverage for claimant login/submission, invalid login, protected routes, and admin status updates.
- Discovered the UI Docker build could omit generated API client files, causing Next.js module-not-found overlays.
- Rebuilt the UI image after regenerating `src/lib/api/generated`.
- Configured Playwright serial execution to avoid Keycloak brute-force lockouts on shared test credentials.
- Confirmed claimant claim creation returns HTTP 201 and subsequent claim list requests return HTTP 200.

### Contract testing

- Added a Pact V4 consumer test for `BFFService -> ClaimsService GET /api/claims`.
- Kept provider verification out of the green suite because it requires seeded provider state and valid authentication.
- The generated pact is committed alongside the test for provider-side verification when state setup is available.

### Final verification

- Fixed separate JWT and contract-test security chains by excluding normal JWT security in `contract-test` profile.
- Added H2 provider-state seed and HTTP contract verification for `GET /api/claims`.
- Final Claims Service suite: 197 passed.
- Final BFF suite: 214 passed.
- Final Vitest suite: 25 files passed.
- Final Playwright suite: 4 Chromium tests passed.
