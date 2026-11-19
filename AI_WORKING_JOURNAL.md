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
