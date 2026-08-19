# Frank!Framework Insights

[![Live Application](https://img.shields.io/badge/Live_Application-insights.frankframework.org-blue)](https://insights.frankframework.org)

The Frank!Framework insights application is an open-source tool designed to provide in-depth insights into the development and release lifecycle of the [Frank!Framework](https://github.com/frankframework/frankframework).


## Purpose of the Application

Frank!Framework Insights provides users, contributors, and maintainers with a centralized overview of the development activities surrounding the Frank!Framework. Instead of manually gathering information from different sources, this tool collects, analyzes, and visualizes key data to make the entire release lifecycle transparent.

The key goal is to offer detailed insights into releases at every stage.

**Past Releases**
<br>
Analyze the composition of previous releases. Understand which issues were fixed, view the final statistics, and identify vulnerabilities.

**Current Release**
<br>
Track the real-time progress of the release currently in development. The dashboard provides insights into the stability and progress by visualizing the current development roadmap with updates about the progress.

**Future Releases (Roadmap)**
<br>
Look ahead at the project's direction. The tool visualizes the roadmap based on GitHub Projects, showing planned features and epics for (upcoming) releases.

### In-depth Release Analysis

For any given release, the tool provides deep-dive analytics by processing a wide range of data points and visualizing the relationships between them. This includes analyzing issue attributes (e.g., bug, feature, priority, and labels), development data such as associated branches and pull requests, and planning elements like milestones and their completion status.

By analyzing the ratios and connections between these elements, users can gain a much deeper understanding of the work involved in a release, identify potential risks, and track the overall health of the development process.

### Security Vulnerability Scanning

The application integrates **Trivy** to automatically scan Frank!Framework release artifacts for known vulnerabilities (CVEs). It provides detailed security information including severity levels, CVSS scores, and vulnerability trends across releases, helping maintainers and users make informed decisions about release security.

<br>

## System Architecture

The backend is split into two independently deployable Spring Boot services that share one PostgreSQL database:

| Service | Responsibility | Port |
| --- | --- | --- |
| **insights-data-import** | Gathers data from external sources (GitHub API, Trivy) and writes it to the database. Runs on a schedule and on a GitHub release webhook. Exposes no read API. | 8081 |
| **insights-webapp** | Reads from the database and serves it through the REST API and the bundled Angular single page application. Never writes imported data. | 8080 |

A third module, **insights-common**, holds what both need: the JPA entities and repositories, the Flyway migrations, the object mapper and the shared HTTP client infrastructure. It is a plain library jar, not a runnable application.

Splitting the two means the import job, which is long running, memory hungry and runs Trivy and Maven as subprocesses, can be restarted, scaled or taken down without touching the site that users are looking at.

Currently, the application primarily uses the **GitHub API** to retrieve data about Frank!Framework's repository. However, the architecture is designed to be extensible, meaning other external data sources can be integrated in a similar way in the future. This allows the application to be scaled with new integrations as needed.

> **Before changing anything substantial, read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).** It covers
> how the modules fit together, how data gets in, and most importantly, the business rules and
> decisions behind the insights. Those cannot be worked out from the code alone.

## Project Structure

The application is a Maven multi-module project with an integrated Angular frontend:

```
insights/
├── docker/
│   ├── Dockerfile                       # One file, two build targets: webapp and data-import
│   └── scripts/                         # Container startup scripts
├── insights-common/                     # Shared library (no main class)
│   └── src/main/
│       ├── java/                        # JPA entities, repositories, mapper, HTTP clients
│       └── resources/
│           ├── db/migration/            # Flyway migrations
│           └── insights-common.properties
├── insights-data-import/                # Gathers data and writes it to the database
│   └── src/main/
│       ├── java/                        # GitHub GraphQL client, *InjectionService, Trivy scanning
│       └── resources/
│           ├── graphql-documents/       # GitHub GraphQL queries
│           └── application*.properties
├── insights-webapp/                     # Serves the data: REST API + frontend
│   └── src/main/
│       ├── java/                        # Controllers, *QueryService, security, rate limiting
│       ├── resources/application*.properties
│       └── frontend/                    # Angular frontend application
│           ├── src/                     # Frontend source code
│           ├── cypress/                 # E2E tests
│           ├── package.json             # Frontend dependencies (pnpm)
│           └── angular.json             # Angular configuration
├── pom.xml                              # Parent POM / reactor
├── docker-compose.yaml                  # Local Docker setup (database + both services)
├── docker-compose.seed.yaml             # Review setup (webapp only, seeded in-memory database)
└── pnpm-lock.yaml                       # pnpm lock file
```

Within a domain package such as `org.frankframework.insights.release`, the classes are divided over
the modules by what they do: the entity and repository live in `insights-common`, everything that
writes (`ReleaseInjectionService`, the GitHub DTOs) in `insights-data-import`, and everything that
reads (`ReleaseController`, `ReleaseQueryService`, `ReleaseResponse`) in `insights-webapp`.

**Build Process:**
1. `insights-common` is built first and both services depend on it
2. While building `insights-webapp`, Maven triggers pnpm to install frontend dependencies
3. Maven triggers pnpm to build the Angular application
4. The built frontend is packaged as static resources inside the `insights-webapp` JAR
5. The webapp serves both the API and the frontend; `insights-data-import` is packaged as a separate JAR

<br>

## Quick Local Setup with Docker

For a fast and easy setup, you can use Docker Compose to run the entire stack: the database, the
import service and the web application.

1.  Ensure you have **Docker Desktop** installed, as it includes Docker and Docker Compose.
2.  Clone the repository:
    ```bash
    git clone https://github.com/frankframework/insights.git
    cd insights
    ```
3.  Fill in your GitHub token, project id, webhook secret and OAuth client in the
    `application-local.properties` of each module. Both services run with the `local` Spring profile.
4.  Build the JARs. The images copy them out of the `target` directories, so Maven has to run first:
    ```bash
    ./mvnw clean package -DskipTests
    ```
5.  Start everything. The `--build` flag forces a rebuild of the images so you are running the latest
    code, and `-d` runs the containers in the background:
    ```bash
    docker compose up -d --build
    ```

| What | Where |
| --- | --- |
| Application (API + frontend) | `http://localhost:8080` |
| Import service health | `http://localhost:8081/actuator/health` |
| GitHub release webhook | `http://localhost:8081/api/webhooks/github` |
| PostgreSQL | `localhost:5432` |

Both services run their own Flyway migrations against the shared database. Flyway locks the schema
history table while migrating, so it does not matter which of the two starts first.

> **Note:** Trivy and Maven are baked into the `insights-data-import` image only (see
> `docker/Dockerfile`), so no additional installation or path configuration is required. The
> `insights-webapp` image is a plain JRE image and does not carry them.

### Running only one of the services

Because the two are independent, you can start just the part you need:

```bash
docker compose up -d insights-webapp        # site only, serves whatever is already in the database
docker compose up -d insights-data-import   # importer only
```

### Database Seeding

If you prefer to start with a clean database and fetch real data from GitHub, configure your GitHub
API token in `insights-data-import/src/main/resources/application-local.properties`, which also sets
`data.fetch-enabled=true`. To browse the application with mock data instead, run the webapp with the
`local-seed` Spring profile, which loads `db/e2e/R__Seed_Data.sql` into an in-memory database.

Please note that not all releases in the mock data set have detailed content. For a full example of
a release with associated issues and pull requests, check release **v9.0.1**.

### Reviewing a pull request with seeded data

`docker-compose.seed.yaml` runs the webapp on its own against that seeded in-memory database, which
is the quickest way to click through someone's changes: no Postgres, no import service, no GitHub
token and no Trivy.

```bash
./mvnw clean package -DskipTests -pl insights-webapp -am
docker compose -f docker-compose.seed.yaml up -d --build
```

The application is at `http://localhost:8080`.

This is a separate Compose project (`insights-seed`), so it never touches the containers or the
Postgres volume of the main `docker compose` stack. Both publish port 8080, so run one at a time.

Because of that project name, **every** command for this stack needs the `-f` flag. A plain
`docker compose ps` looks at the default project and reports an empty list even while the seeded
webapp is running:

```bash
docker compose -f docker-compose.seed.yaml ps
docker compose -f docker-compose.seed.yaml logs -f
docker compose -f docker-compose.seed.yaml down
```

Set `COMPOSE_FILE=docker-compose.seed.yaml` in your shell if you would rather not repeat it, or use
`docker compose ls` to see which projects are running.

Two things to keep in mind while reviewing:

- The database lives in memory. Every restart starts from the same seeded state and anything you
  changed is gone.
- The read-only pages work without logging in. The `/release-manage` pages need a real GitHub OAuth
  login, which the `local-seed` profile cannot provide because its client id is a placeholder.

Without Docker, the same profile works straight from the JAR or from Maven:

```bash
java -jar insights-webapp/target/insights-webapp-*.jar --spring.profiles.active=local-seed
./mvnw spring-boot:run -pl insights-webapp "-Dspring-boot.run.profiles=local-seed"
```

For a frontend pull request you can point the Angular dev server at it: start the backend as above,
then run `pnpm start` in `insights-webapp/src/main/frontend`. The dev server is at
`http://localhost:4200` and `src/proxy.conf.json` forwards `/api`, `/oauth2`, `/login` and
`/actuator` to port 8080.

<br>

## Manual Development Setup

For active development, a manual setup provides more granular control over the individual components. This setup automatically uses the `local` Spring profile for local development configuration.

### Prerequisites

For a manual setup, you will need:

- **Git** - Version control system
- **Java Development Kit (JDK 25)** - Required for the backend (`java.version` in the parent POM)
- **Node.js** (version 24) - Required for the frontend
- **pnpm** (version 10.33.0) - Package manager (`npm install -g pnpm`)
- **PostgreSQL** - Database instance
- **Trivy** - Security vulnerability scanner ([Installation guide](https://aquasecurity.github.io/trivy/latest/getting-started/installation/))
- **IDE** - Recommended: **IntelliJ IDEA**, **WebStorm**, **VS Code**, or **Eclipse**

> **Note on Maven:** A separate installation of Apache Maven is not required. The project includes the Maven Wrapper (`mvnw`), which automatically downloads and uses the correct Maven version.

### Steps

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/frankframework/insights.git
    cd insights
    ```

2.  **Open the Project**

    Open the repository root in your Java IDE. It will detect it as a Maven project with three
    modules. There are two runnable applications:

    | Module | Main class | Profile files |
    | --- | --- | --- |
    | `insights-webapp` | `org.frankframework.insights.InsightsWebappApplication` | `insights-webapp/src/main/resources/application-local.properties` |
    | `insights-data-import` | `org.frankframework.insights.InsightsDataImportApplication` | `insights-data-import/src/main/resources/application-local.properties` |

    Settings that both share (Flyway, JPA, actuator, GitHub URLs) live in
    `insights-common/src/main/resources/insights-common.properties`, which both applications pull in
    through `spring.config.import`. Anything you set in a module's own properties file wins over it.

3.  **Create & Configure the Database**

    Create a single, empty PostgreSQL database. Both services use the same one. Then set the
    datasource properties in **both** `application-local.properties` files:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/your_database_name
    spring.datasource.username=your_username
    spring.datasource.password=your_password
    ```

4.  **Configure the Import Service** (`insights-data-import/src/main/resources/application-local.properties`)

    * **GitHub API access:** create a **GitHub Personal Access Token (PAT)** with the `read:org` and
      `project` permissions ([official guide](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)),
      then set:
      ```properties
      github.graphql.secret=YOUR_PERSONAL_ACCESS_TOKEN_HERE
      github.graphql.project-id=YOUR_GITHUB_PROJECT_ID_HERE
      ```

    * **Initial data injection:** to populate the database with GitHub data on startup, set:
      ```properties
      data.fetch-enabled=true
      ```
      After the first successful run you can set this to `false` to avoid refetching on every start.
      The daily job at midnight runs regardless.

    * **Trivy path:** point at your locally installed Trivy executable:
      ```properties
      trivy.path=C:/Program Files/trivy/trivy.exe
      ```
      > **Note:** only needed for a manual setup. The Docker image ships Trivy on the `PATH`.

5.  **Configure the Web Application** (`insights-webapp/src/main/resources/application-local.properties`)

    Set the GitHub OAuth app used to log users in:
    ```properties
    spring.security.oauth2.client.registration.github.client-id=YOUR_CLIENT_ID
    spring.security.oauth2.client.registration.github.client-secret=YOUR_CLIENT_SECRET
    ```

6.  **Frontend Development (Optional)**

    The frontend is built by Maven as part of `insights-webapp`. For active frontend development with
    live reloading:

    * Navigate to the frontend directory:
        ```bash
        cd insights-webapp/src/main/frontend
        ```
    * Install dependencies:
        ```bash
        pnpm install
        ```
    * Start the development server:
        ```bash
        ng serve
        ```

    The frontend will be available at `http://localhost:4200` with live reloading enabled.

<br>

## Configuration Reference

Settings both services share live in `insights-common/src/main/resources/insights-common.properties`
(Flyway, JPA, actuator, GitHub URLs, and the branch and label filters). Both services import it via
`spring.config.import`; anything a module sets itself wins over it.

> **The `application-local.properties` files are tracked in Git with placeholder values.** Fill in your
> own credentials locally, but never commit real secrets to them.

### Properties you need to set

| Property | Service | What it does                                                                                    |
| --- | --- |-------------------------------------------------------------------------------------------------|
| `spring.datasource.url` / `.username` / `.password` | both | The shared database. Both point at the same one.                                                |
| `data.fetch-enabled` | data-import | Master switch for all GitHub fetching. `false` disables startup, nightly and webhook refreshes. |
| `github.graphql.secret` | data-import | GitHub PAT, needs `read:org` and `project`                                                      |
| `github.graphql.project-id` | data-import | The GitHub Project the roadmap is built from                                                    |
| `insights.webhook.secret` | data-import | Shared secret for GitHub webhook HMAC signatures                                                |
| `trivy.path` | data-import | Path to the Trivy executable. Not needed in Docker, the image ships it on the `PATH`.           |
| `release.archive.directory` | data-import | Where downloaded release zips are cached. Must be persistent storage.                           |
| `trivy.scan.workspace` / `trivy.db.cache` / `maven.local-repo` | data-import | Scratch space, Trivy DB cache, and the Maven repo used to pre-cache dependencies                |
| `spring.security.oauth2.client.registration.github.client-id` / `.client-secret` | webapp | GitHub OAuth app, used to log users in                                                          |
| `cors.allowed.origins[n]` | webapp | Allowed browser origins. Credentials are allowed, so this can never be `*`.                     |
| `frankframework.security.csrf.secure` | webapp | `secure` flag on the CSRF cookie. `false` locally so plain HTTP works, `true` in production.    |

Two settings in `insights-common.properties` are **business rules, not just config**
`github.graphql.branch-protection-regexes` and `github.graphql.includedLabels`. See
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#business-rules) before changing either.

### Environment variables (the `prod` profile)

| Variable | Service |
| --- | --- |
| `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | both |
| `GITHUB_API_SECRET`, `GITHUB_PROJECT_ID`, `INSIGHTS_WEBHOOK_SECRET` | data-import |
| `GITHUB_OAUTH_CLIENT_ID`, `GITHUB_OAUTH_CLIENT_SECRET` | webapp |
| `SERVER_PORT` (optional), `JAVA_OPTS` (optional) | both |

There is **no default Spring profile**. A service started without one has no datasource and will not
boot, always pass `local`, `local-seed` or `prod`.

<br>

## Building & Testing

### Building the Application

Maven builds the three modules in one reactor: `insights-common` first, then the two services.
The Angular frontend is built into the `insights-webapp` JAR.

**Full Build with Tests:**
```bash
./mvnw clean package "-Dspring.profiles.active=local-seed"
```
or
```bash
./mvnw clean install "-Dspring.profiles.active=local-seed"
```

This command:
1. Builds and installs `insights-common`
2. Builds `insights-data-import` and runs its tests
3. Installs frontend dependencies using pnpm and builds the Angular application
4. Compiles `insights-webapp`, runs its tests and the E2E tests
5. Produces two executable JARs:
   - `insights-webapp/target/insights-webapp-<version>.jar` (API + frontend as static resources)
   - `insights-data-import/target/insights-data-import-<version>.jar`

> **Automated Testing:** Running `mvn package` or `mvn install` with the `local-seed` profile automatically executes:
> - **Backend Tests:** JUnit 5 unit and integration tests with Mockito, per module
> - **End-to-End Tests:** Cypress tests using Testcontainers (in `insights-webapp`)
>
> **Important:** The Cypress E2E tests require the `local-seed` Spring profile to seed the database with test data. This is because the E2E tests verify the complete user interface and workflows, which require actual data to be present in the database (releases, issues, pull requests, etc.). Without seeded data, the tests would have nothing to interact with and would fail. The tests run in a containerized environment via Testcontainers, so no additional setup or running application is required.
>
> **Note:** Frontend unit tests (Jasmine/Karma) are not run during the Maven build. To run them separately, see [Running Tests Individually](#running-tests-individually).

**Quick Build (Skip Tests):**

For faster iteration during development, you can skip tests:
```bash
./mvnw clean package -DskipTests
```

**Building a Single Module:**

`-am` ("also make") builds the modules the requested one depends on:
```bash
./mvnw clean package -pl insights-data-import -am        # importer + common
./mvnw clean package -pl insights-webapp -am             # webapp + common (builds the frontend)
```

To skip the pnpm steps while working on the backend only, add `-Dexec.skip=true`.

### Running Tests Individually

**All Backend Tests:**
```bash
./mvnw test
```

**One Module's Tests:**
```bash
./mvnw test -pl insights-data-import
./mvnw test -pl insights-webapp
```

**Frontend Tests Only:**
```bash
cd insights-webapp/src/main/frontend
pnpm test
```

**E2E Tests Interactively:**
```bash
cd insights-webapp/src/main/frontend
pnpm run cypress:open  # Interactive mode with UI
pnpm run cypress:run   # Headless mode
```

### Running the Application

The two services are started separately. The web application works on its own against whatever is
already in the database; you only need the import service when you want to refresh that data.

There is no default profile, so every option below has to pass one. Swap `local` for `local-seed` to
run the webapp against the seeded in-memory database instead of Postgres.

**Option 1 - Run the JARs:**
```bash
java -jar insights-webapp/target/insights-webapp-*.jar --spring.profiles.active=local
java -jar insights-data-import/target/insights-data-import-*.jar --spring.profiles.active=local
```

**Option 2 - IDE:**
Start `InsightsWebappApplication` and/or `InsightsDataImportApplication` directly from your IDE with
the `local` profile active.

**Option 3 - Maven:**
```bash
./mvnw spring-boot:run -pl insights-webapp "-Dspring-boot.run.profiles=local"
./mvnw spring-boot:run -pl insights-data-import "-Dspring-boot.run.profiles=local"
```

The web application will be available at `http://localhost:8080` and the import service at
`http://localhost:8081`. If you're running the frontend development server separately, it will be at
`http://localhost:4200` and proxy API calls to the backend.

<br>

## CI/CD & Quality Assurance

The project uses GitHub Actions to run automated workflows for every pull request and merge to the master branch. These workflows ensure code quality, security, and stability before changes are merged.

### Continuous Integration Pipeline

The CI pipeline (`ci.yaml`) runs the following checks on every pull request:

1. **Code Linting**
   - Backend: Checkstyle for Java code style enforcement (`mvn checkstyle:check`, fails the build)
   - Frontend: ESLint for TypeScript/JavaScript code quality

2. **Code Formatting**
   - Spotless (palantir-java-format) is configured in the parent POM but has **no lifecycle binding**,
     so it does not run in CI and never fails a build. Run it yourself: `mvn spotless:apply` to format,
     `mvn spotless:check` to verify.

3. **Automated Testing**
   - Backend unit and integration tests (JUnit 5)
   - Frontend unit tests (Jasmine/Karma)
   - End-to-end tests (Cypress via Testcontainers)

4. **Build Verification**
   - Full Maven build with pnpm frontend integration
   - Validates that the application can be packaged successfully

### Continuous Deployment

**Docker Image Creation**
<br>
On every merge to master, two Docker images are built from `docker/Dockerfile` (one target each) and
pushed to the GitHub Container Registry:

- `ghcr.io/frankframework/insights-webapp`: API and frontend, plain JRE image
- `ghcr.io/frankframework/insights-data-import`: importer, includes Trivy and Maven for vulnerability scanning

Each image is pushed with three tags: `0.0.<run number>` (the immutable build, matching the Maven
`revision` baked into the JAR), `latest` and `master`. Pin production deployments to the versioned
tag; `latest` always points at the most recent master build.

Both images are signed with cosign, once per digest, so every tag on that digest is covered.
The local `docker compose` build reuses the same two names with a `:local` tag, so a locally built
image never shadows a pulled release.

### Security

The project includes multiple security analysis tools:
- **Trivy:** Scans Frank!Framework release artifacts for CVEs (included in Docker, requires local installation for development)
- **SonarQube:** Tracks code quality metrics, code smells, and security issues

### Performance Testing

**Stress Tests**
<br>
Stress tests can be triggered manually via GitHub Actions (`stress-tests.yaml`) to test the latest version on master. These tests push the system to its limits by simulating high traffic or data load to measure performance, identify bottlenecks, and ensure the application remains stable and responsive under pressure.

<br>


## Contributing

This is an open-source project, and contributions are highly welcome! We follow the overarching contribution guidelines of the Frank!Framework organization.

**Code of Conduct**
<br>
All contributors are expected to adhere to our [Code of Conduct](https://github.com/frankframework/frankframework/blob/master/CODE_OF_CONDUCT.md).

**Contribution Guidelines**
<br>
For general guidelines like commit messages and pull request procedures, see the main [CONTRIBUTING.md](https://github.com/frankframework/frankframework/blob/master/CONTRIBUTING.md).

**Project Structure**
<br>
The project is a Maven multi-module monorepo (`insights-common`, `insights-data-import`,
`insights-webapp`) with an integrated Angular frontend in `insights-webapp/src/main/frontend`. When
working on the frontend, always use **pnpm** as the package manager (not npm or yarn).

When adding backend code, put it in the module that matches what it does: shared entities and
repositories in `insights-common`, anything that writes imported data in `insights-data-import`,
anything that serves data to the frontend in `insights-webapp`. The two services must not depend on
each other, and Flyway migrations always belong in `insights-common`. The reasoning behind those rules
is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#the-rules-that-keep-this-honest).

If you add an API endpoint, add its row to the endpoint table in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#the-api) there is no OpenAPI spec, so that table is the
API documentation.

**Code Conventions**
<br>
- **Frontend:** Must adhere to the [Frank!Framework Frontend Conventions](https://github.com/frankframework/frontend-conventions)
- **Backend:** Document public classes and methods with Javadoc. Follow Checkstyle and Spotless formatting rules

**Quality Requirements**
<br>
Before submitting a pull request:
- Ensure all CI/CD pipeline checks pass (linting, testing, building)
- Clearly explain **what** you changed and **why** in your commit messages and pull request description
- For details on running tests locally, see the [Building & Testing](#building--testing) section

**How to Contribute**
<br>
- Report bugs or suggest features by creating an issue
- Fork the repository and submit a pull request with your improvements
- Improve documentation or add examples

## License

This project is licensed under the Apache 2.0 License. See the `LICENSE` file for the full terms.
