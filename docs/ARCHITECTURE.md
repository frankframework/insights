# How Insights works

The [README](../README.md) tells you how to install and run the app. This file tells you how it works
inside, and, more importantly, the rules you cannot derive from the code.

---

## Table of contents

1. [The short version](#the-short-version)
2. [The three modules](#the-three-modules)
3. [Where the code lives](#where-the-code-lives)
4. [Getting data in](#getting-data-in)
5. [Scanning releases for CVEs](#scanning-releases-for-cves)
6. [Business rules](#business-rules)
7. [Security](#security)
8. [Rate limiting](#rate-limiting)
9. [The API](#the-api)
10. [Configuration and profiles](#configuration-and-profiles)
11. [The frontend](#the-frontend)
12. [Decisions and why](#decisions-and-why)
13. [Known gaps](#known-gaps)

---

## The short version

Insights pulls data about the Frank!Framework from GitHub, stores it in Postgres, and scans every
release for known vulnerabilities with Trivy. A Java / Angular webapp shows all of this data.

Nothing is fetched while you use the app. Everything you see was collected by a background job
beforehand. Loading a page never calls GitHub.

Data is refreshed three ways: when the import service starts, every night at midnight, and when GitHub
says a release was published (webhook). All three do about the same work, and only one can run at a
time.

Since the multi-module split there are **two processes**, not one. One writes the database, one reads
it. They never talk to each other. The database is the only thing between them.

---

## The three modules

| Module | Runs?          | Job |
|---|----------------|---|
| `insights-common` | no (plain jar) | JPA entities, repositories, Flyway migrations, mapper, base HTTP clients, shared enums and properties |
| `insights-data-import` | yes, :8081     | Gathers data from GitHub and Trivy and writes it to the database |
| `insights-webapp` | yes, :8080     | Reads the database and serves the REST API plus the bundled Angular app |

### The rules that keep this honest

Break one of these and you undo the reason the split exists.

1. **The two services must never depend on each other.** Not in Maven, not over HTTP. Shared things go
   in `insights-common`.
2. **`insights-common` never depends on a service.** Arrows point inward only. No main class, no
   controllers there.
3. **Only `insights-data-import` writes imported data.** The webapp writes exactly two things, both
   human input: business values and the manual CVE impact fields.
4. **Migrations live in `insights-common`.** Both services carry the same set and both run Flyway on
   startup. Never put a migration in a service module. It would only run when that service starts, and
   the other would fail `ddl-auto=validate` depending on deploy order.

### Why it is split

The import job is long running, memory hungry, and forks Trivy and Maven as child processes. In one
JVM, a hung Trivy or an OOM in the scanner took the website down with it, the two workloads could not
be resource-capped separately, and deploying a frontend fix restarted a running scan.

Now the importer can be restarted, capped or left dead for a week and the site keeps serving what is
already in the database.

**What it costs:** three modules to reason about; a change that crosses the boundary touches two of
them; two services to deploy and configure; and `insights-common` is a temptation: anything put there
loads into both services, so keep it to entities, repositories, migrations, mappers and properties.

---

## Where the code lives

Package names are identical across modules (`org.frankframework.insights.release` exists in all three).
What differs is which kind of class is in it, and the naming tells you the module:

| Suffix | Module |
|---|---|
| entity, `*Repository` | **common** |
| `*DTO`, `*InjectionService`, `Trivy*` | **data-import** |
| `*Controller`, `*QueryService`, `*Response`, `*Request` | **webapp** |

`release` as the example: `Release` + `ReleaseRepository` in common; `ReleaseDTO`,
`ReleaseInjectionService`, `ReleaseArtifactService` in data-import; `ReleaseController`,
`ReleaseQueryService`, `ReleaseResponse` in webapp.

Things worth knowing about the layout:

- Migrations: `insights-common/src/main/resources/db/migration` (V1 → V1.13).
- Shared settings: `insights-common/src/main/resources/insights-common.properties`, imported by both
  services via `spring.config.import`.
- GraphQL queries we send to GitHub: `insights-data-import/src/main/resources/graphql-documents/`.
- E2E seed data: `insights-webapp/src/main/resources/db/e2e/R__Seed_Data.sql` (`local-seed` only).
- Angular app: `insights-webapp/src/main/frontend/`, compiled into
  `insights-webapp/src/main/resources/frontend/` at build time. That output is gitignored, so never
  commit it.
- The importer has no read API. Its only HTTP surface is the webhook plus actuator health/info.
- Trivy and Maven exist **only** in the data-import image. The webapp image is a plain JRE.

---

## Getting data in

Driver: `insights-data-import/.../common/configuration/SystemDataInitializer.java`.

### Triggers

- **Startup**: implements `CommandLineRunner`.
- **Nightly**: `@Scheduled(cron = "0 0 0 * * *")`, midnight server time.
- **Webhook**: `triggerRefresh()` hands the work to a task executor so the HTTP call returns
  immediately.

All three are skipped when `data.fetch-enabled=false`. That is how tests and CI avoid GitHub.

### Only one job at a time

Two locks, doing different jobs:

- **`isJobRunning`**: an `AtomicBoolean`, stops two jobs in the same JVM.
- **ShedLock** (`@SchedulerLock`): a database lock, stops two jobs across instances.

A webhook arriving during a run is not thrown away: it sets `pendingRefresh` and the running job picks
it up in `drainPendingRefresh()`.

**The escape hatch:** if the in-memory flag has been held longer than two hours, the next job logs an
`ERROR` and takes the lock anyway. Without it, one wedged run would silently block every future refresh
and the app would look healthy while going stale. **If you see that ERROR line, something hung, so
investigate.**

### Order of the steps

```
labels → milestones → issue types → issue project items → branches →
issues → branch pull requests → releases → prune obsolete release zips
```

The order matters: issues cannot be linked to labels and milestones that do not exist yet, and releases
need branches and pull requests. Vulnerability scanning runs afterwards as its own step.

Each step is wrapped by `runInjectionStep()`, which logs a failure and continues with the rest. That is
deliberate: a broken issue import should not stop new releases from landing. **The cost is that
partial failure only shows up in the log.** If the data looks wrong while the app is up, this is the
first place to look.

### The webhook

`POST /api/webhooks/github` on the **importer**, port 8081.

We compute HMAC-SHA256 over the raw body and compare with `MessageDigest.isEqual`, which is
constant-time; a normal string compare would leak the expected signature through timing.

| Situation | Response |
|---|---|
| No secret configured on our side | `500`. Accepting webhooks we cannot verify is worse than failing |
| Missing or wrong signature | `401` |
| Event is not `release` | `200`, ignored |
| `release` action is not `published` | `200`, ignored |
| Body is not valid JSON | `400` |
| Otherwise | `202`, refresh scheduled |

A webhook refresh only scans releases **never scanned before** (`scanUnscannedReleasesOnly()`). A new
release appears fast; rescanning history is the nightly job's task.

---

## Scanning releases for CVEs

`insights-data-import/.../vulnerability/VulnerabilityScanService.java`. Per release:

1. **Download the source zip** from
   `github.com/frankframework/frankframework/archive/refs/tags/<tag>.zip` into
   `release.archive.directory`, and reuse it forever, because release tags never change.
   Downloads go to a `.tmp` file and are only moved into place after the zip is verified openable. A
   truncated download used to leave a file that looked fine and broke every later scan of that release.
   Cached zips are re-verified on reuse.

2. **Unzip with a filter.** Skips extensions and folders that cannot hold dependency info, caps at
   50 000 entries and 2 GB uncompressed, and rejects entries resolving outside the target directory.
   Partly speed, partly zip-bomb and zip-slip defence. *If a technology's dependencies stop showing up,
   check `SKIP_EXTENSIONS` and `SKIP_FOLDERS` first.*

3. **Pre-cache Maven dependencies** with `mvn dependency:resolve` per `pom.xml`, because Trivy can only
   report on dependencies it can resolve. Runs with `--fail-never`: old releases point at repos and
   parent POMs that no longer exist, and one unresolvable module must not kill the scan. *Cost: a scan
   can quietly cover less than you think. If a release reports few findings, read the Maven warnings.*

4. **Run Trivy.**
   ```
   trivy fs --format json --quiet --scanners vuln --vuln-type library
            --skip-db-update --skip-java-db-update --cache-dir <cache> --timeout 60m
   ```
   with `GOMAXPROCS=1` and `TRIVY_OFFLINE_SCAN=true`.

   `--skip-db-update` looks wrong until you see `updateTrivyDatabases()` above it: the databases *are*
   updated, once per run instead of once per release. If that update fails we continue on the cache, because
   slightly stale beats nothing. `GOMAXPROCS=1` stops Trivy claiming every core.

5. **Save findings.** Each hit becomes a `ReleaseVulnerability` row recording *where* it was found:
   target, package name, package path, PURL, installed version, fixed version. Links for the release
   are deleted and rewritten each scan, so fixed CVEs disappear. Then `lastScanned` is set.

Scan order is `lastScanned` ascending, nulls first, so never-scanned releases go first, so a run cut
short has already done the ones that needed it most.

`lastScanned` means "we really looked at this release", and the upgrade advice depends on it.

---

## Business rules

These are product decisions. They look arbitrary until you know why, and none can be worked out from
the code alone.

### Which branches count

Only `^master$`, `^release/[0-9]+\.[0-9]+$` and `^[0-9]+\.[0-9]+-release$`
(`github.graphql.branch-protection-regexes`). Everything else is ignored, because a feature branch has no
release and no lifecycle, and importing them would pull in tens of thousands of irrelevant pull
requests. Two naming conventions are listed because the convention changed and old branches still use
the old form. **Branches that do not match are invisible and nothing warns you.**

### Which releases count

Names matching `-RC<digits>` or `-B<digits>` are not releases (`ReleaseDTO.isValid()`). Users care
about what they can run in production, and RCs would triple the nodes on the timeline.

A release's date is the **tag commit date**, not the GitHub publication date. The publication
timestamp moves when someone edits release notes months later, the commit date never does. Everything
downstream depends on that stability.

### Which pull requests belong to a release

Releases in a branch are sorted oldest first; a release gets every PR merged in
`[previous release date, this release date)`. The first release in a branch gets none, because it only opens
the window for the next one. GitHub does not tell you which release a PR shipped in, so merge time
between two tags is the closest honest approximation.

**The beta/RC exception:** if a release had betas or RCs, its window closes at the earliest beta/RC
date instead. Work merged after the first beta is stabilisation for the *next* version, not content of
this one.

**Master rollup:** each branch's earliest release is folded into master's timeline and master's PRs are
distributed over the combined list. A release branch is cut from master, so its first release must be
positioned inside master's history to get its content.

### Deleted releases and `lastScanned`

Releases no longer returned by GitHub are deleted with their links, and their zips pruned. But
`lastScanned` is read back and re-applied on every import. Without that, each nightly import would
reset the scan clock and trigger a full rescan of all history the same night.

### The count check that skips a fetch

Labels, issue types and branches are only re-fetched when GitHub's count differs from the row count;
issue project items only when their tables are empty. Cheap, and these sets are nearly static.

**Known limitation:** the check is on *count*, not content. Rename or recolour a label, or delete one
and add another the same day, and nothing updates. Issue project items are never refreshed once
non-empty, so a new priority or status option in the GitHub Project will not appear. Fix: clear the
table and let the next run repopulate it.

### Release highlights are filtered by label colour

Highlights are a release's labels filtered to four hex colours (`FEF2C0`, `D4C5F9`, `006B75`,
`C5DEF5`), ordered by how often they occur. Filtered by **colour, not name**, because label names in
the framework repo are renamed regularly while colours are stable. The colour is what maintainers use
to signal a category.

**This is fragile and everyone knew it.** Recolour a label in GitHub and it silently drops out of every
release page. If highlights go empty, check the label colours first. Note the filter is applied on
**read** (`LabelQueryService`), not on import, so changing the list takes effect without a re-import.

### Which issues are shown

Only **root** issues (anything that is somebody's sub-issue appears nested instead), and only if it or
any issue in its sub-issue tree carries one of those coloured labels. An unlabelled issue is internal
bookkeeping. Points total = own points + all sub-issue points, with **3** for issues that have none, since
treating unpointed work as zero would make a busy release look empty. A points total is always an
estimate; do not present it as exact.

`/api/issues/future` returns issues of type `Epic` with **no milestone**, because assigning a milestone is
the moment planned work becomes scheduled work. (The javadoc claims it filters on a due date; it does
not. The query is the truth, and the literal `"Epic"` must match the GitHub issue type name exactly.)

### Version families

Releases group into families by the first two version parts, so `v7.7.1` and `v7.7.4` are both `7.7`.
Key derivation: from the tag (`^v?(\d+\.\d+)\.\d+`), else the branch name minus `release/`, else
`unknown`. Only keys matching `^\d+\.\d+$` are "versioned". `master` and `unknown` are never offered as
upgrade targets, because "upgrade to master" is not advice anyone can act on.

A release is a **nightly** if `nightly` appears in its tag or branch name. This is implemented three
times (once in Java, twice in TypeScript). Change the naming convention and all three need updating.

### Upgrade advice

Two different questions, deliberately different rules.

**Short-term fix: stay on your branch, take a patch.** Per affected family, find the last affected
release, then look forward in that family for the newest release that is not a nightly, is in a
versioned family, **and has been scanned**.

That last condition is the important one: we only recommend a build we actually scanned and found
clean. A newer unscanned release could carry the same CVE, and suggesting it would be a guess presented
as security advice. **Consequence to accept:** for a few hours after a release is published the
short-term fix can be empty, because the new build has not been scanned yet.

**Long-term fix: where should we end up.** One suggestion for the whole CVE. Take the highest affected
family, then walk every family *not newer* than it, newest first, and pick the first with a versioned
non-nightly release after its own last affected one.

It deliberately does **not** require `lastScanned`: this answers "which branch to move to", not "this
build is clean". Requiring a scan would empty the field for just-released branches, exactly when
people need it. And "not newer than the highest affected" is there so we do not conflate "this fixes
your CVE" with "this is simply newer".

**The affected list** prefers versioned non-nightly releases, but falls back to the unfiltered list if
that leaves it **empty**. Some CVEs only ever appear in nightlies, and an empty list on a CVE that
clearly affects something reads as a bug.

### Severity, CVSS score and vector

Trivy returns CVSS data from several providers. We resolve **one** primary source per vulnerability
(`nvd` → `redhat` → first other present) and read both score and vector from that same entry. If the
score came from NVD and the vector from Red Hat, the calculator in the UI would recompute a number
that does not match the one displayed beside it.

Score falls back: primary V3 → primary V2 → already-stored → a representative score from severity
(CRITICAL 9.5, HIGH 8.0, MEDIUM 5.5, LOW 2.0, else 0.0). Sorting needs a number for every row;
these are mid-band placeholders to sort correctly, **not** real CVSS scores.

The vector keeps its old value if a scan returns none, and `publishedAt` is only ever set once, because later
scans sometimes return less metadata, and overwriting good data with null would be a regression. Rows
scanned before `cvssVector` existed stay null until Trivy rescans them; harmless, no backfill needed.

### Manual impact assessment

An org member can attach `impactScore` and `impactDescription` to a CVE. **The scanner never writes or
overwrites these.** A CVE in a dependency is not automatically a vulnerability in the Frank!Framework, because
the vulnerable path may be unreachable. Only a human can judge that, and that judgement is the most
valuable content on the CVE page, so a tool must never clobber it. The CVSS calculator
(`components/cvss-calculator/`, `pipes/cvss.ts`) prefills from the stored vector so the assessor only
adjusts what differs.

### The release support policy

Counted from the first non-nightly release in a branch:

| Release type | Full support | Security support |
|---|---|---|
| Major (`x.0`) | 6 months | 12 months |
| Minor (`x.y`) | 3 months | 6 months |

`master` and nightly branches always count as maintained. Majors get longer because upgrading across a
major is a project.

Implemented **twice**: `ReleaseNodeService.getSupportEndDates()` uses both windows for the graph
colours; `isBranchMaintained()` in `pipes/branch-lifecycle.ts` uses only the **security** window to
decide which branches the CVE overview shows by default. A branch out of full support still gets
security fixes, so its CVEs are actionable; one out of security support will never be fixed, and
showing it by default would flood the page. The toggle brings them back.

**Change the policy and you must change both files.** They also classify slightly differently:
`branch-lifecycle.ts` treats `minor === 0` as major, `ReleaseNodeService` requires
`patch === 0 && minor === 0`.

### Graph colours

Applied in layers; later layers overwrite earlier ones, so the call order in `assignReleaseColors()`
**is** the rule:

| Order | Colour | Meaning |
|---|---|---|
| 0 | Grey (historical) | starting point, everything begins here |
| 1 | Red (EOL) | branch has a nightly and is past its security window |
| 2 | Blue (supported) | still inside its security window |
| 3 | Purple (LTS) | latest major version |
| 4 | Yellow (bleeding edge) | latest nightly on `master` |
| 5 | Green (latest stable) | newest stable release, **unless** it is already the LTS |

Green is skipped when the latest stable is also the latest major, because purple *and* green on one node makes
the legend meaningless. Only the latest release and latest nightly in a branch get a colour; the graph
is about what is current. EOL also requires a nightly, because a branch that never had one was never
actively developed.

### Business values

The only feature where users create data. Free-text statements of why a release mattered, linked to the
issues that delivered them.

- **Release-scoped.** Titles are unique **per release**, not globally. "Performance" can legitimately
  be a business value of 8.0 and again of 9.0. Enforced twice: a composite unique constraint and an
  explicit service check that returns a readable error.
- **Deleting one disconnects its issues; the issues survive.** Issues are imported data owned by the
  importer; user grouping must never delete them.
- **`PUT /{id}/issues` replaces the whole set**, and fails entirely with a 404 if any issue id is
  unknown. The UI is a multi-select showing the complete desired state, so replace matches what the
  user sees, and all-or-nothing prevents a half-applied selection.
- **Duplicating into another release copies title and description only.** Issue links are not carried
  over, because those issues shipped in the *source* release, and copying them would claim the same work
  twice. Existing titles in the target are skipped.

---

## Security

**Login.** GitHub OAuth2, scopes `read:user` and `read:org`. Anyone can log in; **only members of the
`frankframework` organisation can do anything.** Non-members get a `403` with an explicit message
rather than a silent failure. Membership already lives in GitHub, so we read it instead of maintaining
a second user list, so nobody has to remember to revoke access here when someone leaves.

This is a *different* credential from the PAT used to fetch data: the PAT reads GitHub (importer), the
OAuth app logs users in (webapp). A full local setup needs both, in two different property files.

**Endpoint access** (`SecurityConfig`, first match wins):

| Endpoints | Access |
|---|---|
| `/api/business-value/release/**`, `/api/vulnerabilities/release/**`, `/api/vulnerabilities/detailed` | public |
| `/api/auth/user`, rest of `/api/business-value/**` and `/api/vulnerabilities/**` | authenticated |
| everything else | public |

The public rules are the more specific ones and sit above the authenticated ones deliberately, because they
are what the public site needs. **Add a read endpoint under one of those prefixes and it will require
login unless you also add it to the public list.** That default is on purpose.

The frontend's `FrankFrameworkMemberGuard` only stops us showing pages that would not work. It is
convenience, not security. Every check that matters happens on the server.

**Sessions, not JWT.** Access depends on org membership, and membership can be revoked. A session can
be killed immediately, a JWT stays valid until it expires. Three concurrent sessions per user; a fourth
login ends the oldest rather than locking anyone out. Session ids are regenerated at login.

**CSRF** in a `SECURE-XSRF-TOKEN` cookie (`httpOnly`, `SameSite=Lax`). The `secure` flag comes from
`frankframework.security.csrf.secure` so local HTTP works. Webhook paths are excluded, because GitHub cannot
send a CSRF token and the signature already proves the request.

Also `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, and a CSP that is
**report-only**. CORS origins come from `cors.allowed.origins[n]`;
credentials are allowed so the list can never be `*`.

---

## Rate limiting

This limits **failures**, not traffic: five failed requests per fifteen minutes keyed on GitHub login.
A response ≥400 consumes a token; **any success empties the bucket completely.** So normal use is never
affected: you would have to fail five times in a row with no success in between. Anonymous requests
are not limited, because the key is the GitHub login.

Buckets live in a `ConcurrentHashMap`, cleaned nightly by `RateLimitCleanupConfiguration`. Per
instance, not shared. Fine for what this protects, but it is not a security boundary.

**Read this before changing anything here.** Two path filters disagree:

- `WebMvcConfiguration` registers the interceptor on `/api/auth/**` and `/api/business-value/**` only.
- `RateLimitInterceptor.shouldSkipRateLimiting()` also allows `/api/vulnerabilities`.

Registration wins, so **the vulnerability endpoints are not rate limited today**, even though the
interceptor reads as if they were. Decide which was intended and make both agree.

---

## The API

`ApiPrefixConfiguration` adds `/api` to every `@RestController`, so `@RequestMapping("/releases")` is
served at `/api/releases`. Do not write the prefix yourself or you get `/api/api/`. Security rules and
the interceptor *do* use full paths. Both service modules have their own copy of this class, so the
webhook keeps the `/api/webhooks/github` path it had before the split and GitHub needed no
reconfiguration.

All on the webapp (8080) except the last, which is on the importer (8081):

| Method | Path | Auth |
|---|---|---|
| GET | `/api/auth/user` | yes |
| POST | `/api/auth/logout` | no |
| GET | `/api/releases` | no |
| GET | `/api/releases/{releaseId}` | no |
| GET | `/api/issues/release/{releaseId}` | no |
| GET | `/api/issues/milestone/{milestoneId}` | no |
| GET | `/api/issues/future` | no |
| GET | `/api/labels/release/{releaseId}` | no |
| GET | `/api/milestones` | no |
| GET | `/api/vulnerabilities` | yes |
| GET | `/api/vulnerabilities/detailed` | no |
| GET | `/api/vulnerabilities/release/{releaseId}` | no |
| PUT | `/api/vulnerabilities/{cveId}/impact` | yes |
| DELETE | `/api/vulnerabilities/{cveId}/impact` | yes |
| GET | `/api/business-value/release/{releaseId}` | no |
| GET | `/api/business-value/{id}` | yes |
| POST | `/api/business-value` | yes |
| PUT | `/api/business-value/{id}` | yes |
| DELETE | `/api/business-value/{id}` | yes |
| PUT | `/api/business-value/{id}/issues` | yes |
| POST | `/api/business-value/release/{targetReleaseId}/duplicate` | yes |
| POST | `/api/webhooks/github` | HMAC signature (importer, :8081) |

There is no OpenAPI spec. **This table is the API documentation, so add the row when you add an
endpoint.**

`/api/vulnerabilities/detailed` is the only paginated endpoint: `?page=`, `?size=` (default 20),
optional `?search=` matched case-insensitively on CVE id, title and description. Sorting is fixed:
`publishedAt` desc (nulls last), then `cveId` asc. (The controller javadoc still claims CVSS score
desc; the code is the truth.)

**Errors** from `GlobalExceptionHandler` all have one shape, with `messages` always a list because one
validation failure can produce several:

```json
{ "httpStatus": 403, "messages": ["Access denied. ..."], "errorCode": "Forbidden" }
```

**Serving the frontend.** `spaRouter()` returns `index.html` for anything that is not `/api/**`,
`/error`, or a path with a **non-numeric** extension. `/api/**` must keep returning JSON 404s and
`/error` must stay free for Spring. The non-numeric part is subtle: `v9.0.0` ends in `.0`, which would
otherwise be read as a file extension, so `/graph/v9.0.0` would 404 instead of loading the app.

---

## Configuration and profiles

**There is no default profile.** Start a service without one and it has no datasource and will not
boot. That is deliberate: any default would either silently point production at localhost or at a
throwaway in-memory database.

| Profile | Database | GitHub fetching | Used for |
|---|---|---|---|
| `local` | Postgres on localhost / `DB_HOST` | **on** in the importer | development, and `docker compose` |
| `local-seed` | H2 in memory, seeded from `db/e2e` | off | E2E tests, CI, Sonar, reviewing a PR |
| `prod` | Postgres from env vars | on | deployment |

`local-seed` is in-memory, so changes vanish on restart, and every test run starts from the same state.
That is also what makes it the review profile: `docker-compose.seed.yaml` starts the webapp alone on this
profile, without Postgres, the importer, a GitHub token or Trivy. See the
[README](../README.md#reviewing-a-pull-request-with-seeded-data).

Two settings in `insights-common.properties` are real decisions, not just config:
`github.graphql.branch-protection-regexes` and `github.graphql.includedLabels`, both explained under
[Business rules](#business-rules).

Property and environment-variable reference is in the [README](../README.md#configuration-reference).

---

## The frontend

Angular 22, standalone components, signals. No NgRx, because component signals plus a few services are
enough for a read-mostly app where each page owns its data. OnPush is the default in this Angular
version, so components carry no `changeDetection` setting; that is correct, do not "fix" it.

Managed with **pnpm** (workspace at the repo root, single lockfile). Not npm, not yarn.

| Route | Guarded |
|---|---|
| `/graph`, `/graph/:id` | no |
| `/roadmap` | no |
| `/cve-overview`, `/cve-overview/:cveId` | no |
| `/vulnerabilities/manage[/:cveId]` | yes |
| `/release-manage/:id` and children | yes |

`/` redirects to `/graph`. `ng serve` on 4200 proxies `/api`, `/oauth2`, `/login` and `/actuator` to
`localhost:8080` (`src/proxy.conf.json`).

**Four places to be careful:**

- **`pages/release-graph/`**: the biggest thing in the app, ~2 600 lines across the component and its
  node and link services. A hand-built timeline: dates mapped to pixels, one lane per branch,
  mini-nodes for collapsed groups, plus the colour rules above. No layout library underneath, so a
  change here often breaks something hundreds of lines away. The ~2 900 lines of spec are the safety
  net.
- **`pipes/cvss.ts`**: a full CVSS v3.1 implementation matching the FIRST.org spec. The constants and
  rounding are not arbitrary. If a score looks wrong, check the spec before changing anything.
- **`pipes/release-range.ts`**: merges version ranges for the affected-versions display. Many edge
  cases; the tests cover them, so run them.
- **`pipes/branch-lifecycle.ts`**: one half of the duplicated support-window rule described above.

---

## Decisions and why

The module split is covered under [The three modules](#the-three-modules). The rest:

**One jar with the frontend inside.** Maven runs pnpm and packages the Angular build as static
resources. One artefact, one origin, no CORS or proxy setup in production, and the API and app can
never be at mismatched versions. Cost: a frontend-only change still needs a Maven build, which is why
`ng serve` and `-Dexec.skip=true` exist.

**Everything is fetched up front.** The GitHub API is rate limited and slow. On-demand fetching would
make pages unpredictable and a busy day could exhaust the rate limit and take the site down. Cost:
data can be 24 hours stale, which the release webhook fixes for the case people notice.

**Trivy instead of GitHub's own vulnerability data.** We need what is in the released artefact, not the
repo's current dependency graph. Dependabot describes `master` today; a user is running 8.1.2 from
eighteen months ago. Scanning the release zip answers the question users are actually asking, and lets
us scan releases that predate any alerting.

**Flyway plus `ddl-auto=validate`.** `update` silently alters production tables from whatever the
entities happen to say and produces a schema nobody can reproduce. `validate` turns a mismatch into a
loud startup failure. Cost: every field addition needs a migration, which is the point.

**Jackson `convertValue` for mapping, not MapStruct.** Field names match almost everywhere, so no
mapping code, no annotation processor, no generated sources. Cost: mapping errors are runtime, not
compile time. Rename a field on one side and the other silently gets null. Practical consequence:
**append new fields to the end of response records.** Records match positionally, so inserting in the
middle silently re-indexes every positional constructor call in the tests.

**Sessions instead of JWT**: see [Security](#security).

**No OpenAPI.** The API is small, internal, and consumed by exactly one frontend in the same repo,
released together. A generator plus annotations on every controller has not paid for itself. Cost: the
table above is maintained by hand and will rot if nobody updates it. Revisit the moment anything
outside this repo consumes the API.

**Stay on current versions** (Java 25, Spring Boot 4.1, Angular 22), kept moving by Renovate. The
project is young with no external API consumers, so staying current is cheap and falling behind
compounds. When bumping Java, four places must move **together**: the Docker base image, the CI
`JAVA_VERSION`, the Sonar workflow's JDK, and `java.version` in the parent POM.
