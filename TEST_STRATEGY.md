# Test Strategy and Application Assessment

## Baseline setup

- Infrastructure and application containers are running successfully.
- UI: http://localhost:3001/login
- Claims health: http://localhost:8080/actuator/health
- BFF health: http://localhost:8090/actuator/health
- Baseline commit created before test changes.

## Highest-risk areas

1. **Authentication and authorization** — JWT issuance, role enforcement, admin/claimant access, and logout/refresh flows protect financial and personal claim data.
2. **Claim state transitions** — invalid transitions or unauthorized updates could corrupt claim records or dashboard totals.
3. **BFF to Claims Service integration** — brittle proxy, token refresh, HTTP error mapping, and correlation/tracing behaviour.
4. **Kafka and real-time updates** — event payloads, ordering, duplicate delivery, CDC/direct-Kafka switching, and WebSocket fan-out.
5. **Frontend claim wizard** — validation, state persistence, and submission data affect the primary user journey.

## Existing test baseline

- Baseline missing constructor dependency was fixed in `UpdateClaimStatusUseCaseTest`.
- The Claims Service suite has 197 tests with remaining failures in `CreateClaimUseCaseTest` and `KafkaDomainEventPublisherTest` fixtures/assertions.
- The BFF suite passed in the baseline run.
- The frontend suite initially could not resolve generated API clients. API generation restored those suites.
- Frontend baseline: 24 test files pass and `wizard-step-2.test.tsx` has 7 assertion failures.
- Theme persistence had a real implementation mismatch: the store used `sessionStorage` while persistence tests assumed `localStorage`; changed to `localStorage`.
- Frontend dependencies report npm audit vulnerabilities; this is a security-quality finding for later review.

## Test prioritisation

### Prioritize

- Claim creation and status transition use cases
- User/admin role enforcement
- User-facing claim wizard behaviour and validation
- BFF authentication, RBAC, and Claims Service adapter
- End-to-end claimant create claim / admin view and update journey
- Direct Kafka and WebSocket event delivery
- Contract tests for BFF and Claims Service API boundaries

### Deliberately defer

- Full exhaustive UI component coverage
- Visual regression testing
- Load testing, until core correctness and E2E coverage are stable
- CDC mode testing until direct-Kafka mode is baseline-tested

## Issues and quality concerns

- Windows Controlled Folder Access prevented writes and Git initialization in the original `Documents` workspace. Development and builds continue from `C:\Users\RASHMI\demo-app-qa-main`.
- Claims and BFF OpenAPI generation works outside the protected folder.
- Some existing tests contain stale constructor dependencies and mock assumptions.
- Several frontend suites depend on generated API files and will fail unless `npm run generate:api` is run first.

## End-to-end coverage

- Playwright Chromium tests cover claimant login, valid claim submission, invalid login, protected-route redirect, and admin status updates.
- Tests run serially because shared Keycloak test accounts may trigger brute-force protection under parallel login attempts.
- The UI Docker image must include regenerated TypeScript API client files under `src/lib/api/generated`.
- Browser navigation from `My Claims` to `Submit New Claim` was also exercised manually and should be investigated as a UI/route issue if it remains unreliable outside direct route navigation.

## Contract testing

- A Pact consumer test covers `BFFService -> ClaimsService GET /api/claims`.
- The generated pact is stored under `code/bff-service/pacts/BFFService-default.json`.
- Provider verification remains pending because it requires seeded submitted claims and valid provider authentication/state setup.
