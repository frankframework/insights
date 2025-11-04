# Frank!Framework Insights

[![Live Application](https://img.shields.io/badge/Live_Application-insights.frankframework.org-blue)](https://insights.frankframework.org)

The Frank!Framework insights application is an open-source tool designed to provide in-depth insights into the development and release lifecycle of the [Frank!Framework](https://github.com/frankframework/frankframework).


## Purpose of the Application

Frank!Framework Insights provides users, contributors, and maintainers with a centralized overview of the development activities surrounding the Frank!Framework. Instead of manually gathering information from different sources, this tool collects, analyzes, and visualizes key data to make the entire release lifecycle transparent.

The key goal is to offer detailed insights into releases at every stage.

**Past Releases**
<br>
Analyze the composition of previous releases. Understand what types of issues were resolved and view final metrics.

**Current Release**
<br>
Track the real-time progress of the release currently in development. The dashboard provides insights into the stability and progress by visualizing the current development roadmap with updates about the progress.

**Future Releases (Roadmap)**
<br>
Look ahead at the project's direction. The tool visualizes the roadmap based on GitHub Projects, showing planned features and epics for upcoming releases.

### In-depth Release Analysis

For any given release, the tool provides deep-dive analytics by processing a wide range of data points and visualizing the relationships between them. This includes analyzing issue attributes (e.g., bug, feature, priority, and labels), development data such as associated branches and pull requests, and planning elements like milestones and their completion status.

By analyzing the ratios and connections between these elements, users can gain a much deeper understanding of the work involved in a release, identify potential risks, and track the overall health of the development process.

<br>

## System Architecture

The backend fetches data from external APIs, processes it, and stores it in a database. The backend then serves this data to the frontend, where it is visualized for the user.

Currently, the application primarily uses the **GitHub API** to retrieve data about the Frank!Framework's development. However, the architecture is designed to be extensible, meaning other external data sources can be integrated in a similar way in the future. This allows the application to be scaled with new integrations as needed.

![Insights-components-without-text](https://github.com/user-attachments/assets/278d03eb-d230-4246-93fe-705b0343dce6)
<p align="start">
  <em>A high-level overview of the data flow from external APIs to the user.</em>
</p>

<br>

## Project Structure

The application is structured as a single Maven project with an integrated Angular frontend:

```
insights/
├── docker/
│   ├── Dockerfile                  # Container definition
│   └── scripts/                    # Container startup scripts
├── src/
│   ├── main/
│   │   ├── java/                   # Backend Java source code
│   │   ├── resources/              # Backend configuration and static resources
│   │   └── frontend/               # Angular frontend application
│   │       ├── src/                # Frontend source code
│   │       ├── cypress/            # E2E tests
│   │       ├── package.json        # Frontend dependencies (pnpm)
│   │       └── angular.json        # Angular configuration
│   └── test/
│       └── java/                   # Backend tests
├── pom.xml                         # Maven build configuration
├── docker-compose.yaml             # Local Docker setup
└── pnpm-lock.yaml                  # pnpm lock file
```

**Build Process:**
1. Maven triggers pnpm to install frontend dependencies
2. Maven triggers pnpm to build the Angular application
3. The built frontend is packaged as static resources in the JAR
4. Spring Boot serves both the API and the frontend from a single application

<br>

## Quick Local Setup with Docker

For a fast and easy setup, you can use Docker Compose to run the entire application stack. This is the recommended method for most users.

1.  Ensure you have **Docker Desktop** installed, as it includes Docker and Docker Compose.
2.  Clone the repository:
    ```bash
    git clone https://github.com/frankframework/insights.git
    cd insights
    ```
3.  From the root of the project, run the following command. The `--build` flag forces a rebuild of the images to ensure you are running the latest version of the code, and the `-d` flag runs the containers in "detached mode" in the background.
    ```bash
    docker compose up -d --build
    ```

The application will be available at `http://localhost:8080`. The backend serves both the API and the frontend application as static resources.

### Database Seeding

By default, the application automatically seeds the database with mock data on startup when running via Docker. This allows you to explore the features immediately without needing to configure GitHub API access or fetch real data.

Please note that not all releases in the mock data set have detailed content. For a full example of a release with associated issues and pull requests, check release **v9.0.1**.

If you prefer to start with a clean, empty database and fetch real data from GitHub, you will need to configure your GitHub API token in the application properties and set `github.fetch=true`.

<br>

## Manual Development Setup

For active development, a manual setup provides more granular control over the individual components. This setup automatically uses the `local` Spring profile for local development configuration.

### Prerequisites

For a manual setup, you will need:

- **Git** - Version control system
- **Java Development Kit (JDK 21)** - Required for the backend
- **Node.js** (version 23 or higher) - Required for the frontend
- **pnpm** (version 10.4.0 or higher) - Package manager (`npm install -g pnpm`)
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

2.  **Backend Setup**
    * **Open Project:** Open the project root directory in your Java IDE. It will automatically detect it as a Maven project.

    * **Spring Profile:** The application is pre-configured to use the `local` Spring profile by default (set in `application.properties`). This automatically loads configuration from `application-local.properties` for your local environment. No additional configuration is needed.

    * **Create & Configure Database:**
        * Create a new, empty database in your PostgreSQL instance.
        * Open `src/main/resources/application-local.properties`.
        * Update the datasource properties with your database credentials:
            ```properties
            spring.datasource.url=jdbc:postgresql://localhost:5432/your_database_name
            spring.datasource.username=your_username
            spring.datasource.password=your_password
            ```

	* **Initial Data Injection:**
	  To populate your database with GitHub data on first run, set:
	    ```properties
		github.fetch=true
		```
	  After the first successful run, set this to `false` to avoid refetching on every startup.

    * **Configure GitHub API Access:**
        The application needs a GitHub token to fetch data from the Frank!Framework organization.
        * Create a **GitHub Personal Access Token (PAT)** with permissions: `read:org`, `project`. Follow the official guide: [Managing your personal access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens).
        * Add your token and GitHub Project ID to `application-local.properties`:
            ```properties
            github.token=YOUR_PERSONAL_ACCESS_TOKEN_HERE
            github.project.id=YOUR_GITHUB_PROJECT_ID_HERE
            ```

3.  **Frontend Development (Optional)**

    The frontend is automatically built by Maven during the build process. However, for active frontend development with live reloading:

    * Navigate to the frontend directory:
        ```bash
        cd src/main/frontend
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

4.  **Run the Application**

    * **Option 1 - Full Build:** Build the complete application (backend + frontend) using Maven:
        ```bash
        mvn clean package -DskipTests
        ```
        Then run the generated JAR:
        ```bash
        java -jar target/insights-*.jar
        ```

    * **Option 2 - IDE:** Start the Spring Boot application directly from your IDE.

The application will be available at `http://localhost:8080`. If you're running the frontend separately, it will be at `http://localhost:4200` and proxy API calls to the backend.

<br>

## Automated Workflows & Quality Assurance

The project uses a comprehensive suite of automated workflows to ensure code quality, security, stability, and correctness. These workflows run in the CI/CD pipeline for every pull request and merge to the master branch.

### Code Quality

**Linting**
<br>
Static code analysis is performed on both the backend (Checkstyle) and frontend (ESLint) to enforce consistent coding styles and catch common programming errors before they are merged.

**Code Formatting**
<br>
Automatic code formatting with Spotless for Java ensures consistent code style across the backend codebase.

### Testing

**Unit & Integration Tests**
<br>
Automated tests verify the functionality of individual components (unit tests) and their interactions (integration tests). This forms the core of our regression testing strategy. The backend uses JUnit 5 and Mockito, while the frontend uses Jasmine and Karma.

**End-to-End (E2E) Tests**
<br>
Cypress tests simulate real user scenarios by running automated tests against the fully built application. These tests verify that the integrated system works as expected from the user's perspective. E2E tests are automatically run during the Maven build process using Testcontainers.

To run E2E tests locally:
```bash
cd src/main/frontend
pnpm run cypress:open  # Interactive mode
pnpm run cypress:run   # Headless mode
```

### Build & Deployment

**Build**
<br>
The workflow compiles the source code, builds the frontend with pnpm, runs all tests, and packages the application into a single executable JAR file. The frontend is bundled as static resources served by the Spring Boot backend.

**Docker Image Creation**
<br>
On every merge to master, a Docker image is automatically built and pushed to the GitHub Container Registry. The image includes both the application and Trivy for vulnerability scanning.

### Security

**Vulnerability Scanning**
<br>
Trivy is integrated into the application to scan frankframework release artifacts for security vulnerabilities. The Docker image includes Trivy, and for local development, Trivy must be installed separately. The application uses Trivy to analyze Java JAR files and identify known CVEs in dependencies.

### Performance

**Stress Tests**
<br>
These tests push the system to its limits by simulating high traffic or data load. The goal is to measure performance, identify bottlenecks, and ensure the application remains stable and responsive under pressure. Stress tests can be triggered manually via GitHub Actions to test the latest version on master.

<br>


## Contributing

This is an open-source project, and contributions are highly welcome! We follow the overarching contribution guidelines of the Frank!Framework organization. Before you start, please familiarize yourself with them.

**Code of Conduct**
<br>
All contributors are expected to adhere to our [Code of Conduct](https://github.com/frankframework/frankframework/blob/master/CODE_OF_CONDUCT.md).

**Contribution Guidelines**
<br>
For general guidelines like commit messages and pull request procedures, see the main [CONTRIBUTING.md](https://github.com/frankframework/frankframework/blob/master/CONTRIBUTING.md).

**Getting Started**
<br>
The project is a Maven monorepo with an integrated Angular frontend located in `src/main/frontend`. When working on the frontend, always use **pnpm** as the package manager (not npm or yarn). The Maven build automatically handles frontend building via pnpm.

**Frontend Conventions**
<br>
All frontend code must adhere to the [Frank!Framework Frontend Conventions](https://github.com/frankframework/frontend-conventions). The frontend is located in `src/main/frontend` and uses Angular with TypeScript.

**Backend Conventions**
<br>
New public classes and methods in the Java backend should be documented with **Javadoc**. Follow the existing code style enforced by Checkstyle and Spotless.

**Testing**
<br>
All new code must pass the existing tests and should be covered by new tests where applicable. This includes:
- Backend: JUnit 5 unit and integration tests
- Frontend: Jasmine/Karma unit tests
- E2E: Cypress tests for user-facing features

**Running Tests**
<br>
```bash
# Run all tests (backend + frontend + E2E)
./mvnw clean verify

# Run only backend tests
./mvnw test

# Run only frontend tests
cd src/main/frontend && pnpm test

# Run E2E tests
cd src/main/frontend && pnpm run cypress:open
```

You can contribute by reporting bugs or suggesting new features by creating an issue, or by forking the repository and submitting a pull request with your improvements.

## License

This project is licensed under the Apache 2.0 License. See the `LICENSE` file for the full terms.
