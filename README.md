# Frank!Framework Insights

[![Live Application](https://img.shields.io/badge/Live_Application-insights.frankframework.org-blue)](https://insights.frankframework.org)

The Frank!Framework insights application is an open-source tool designed to provide in-depth insights into the development and release lifecycle of the [Frank!Framework](https://github.com/frankframework/frankframework).

## Purpose of the Application

Frank!Framework Insights provides users, contributors, and maintainers with a centralized overview of the development activities surrounding the Frank!Framework. Instead of manually gathering information from different sources, this tool collects, analyzes, and visualizes key data to make the entire release lifecycle transparent.

The key goal is to offer detailed insights into releases at every stage.

**Past Releases**
Analyze the composition of previous releases. Understand what types of issues were resolved and view final metrics.

**Current Release**
Track the real-time progress of the release currently in development. The dashboard provides insights into the stability and progress by visualizing the current development roadmap with updates about the progress.

**Future Releases (Roadmap)**
Look ahead at the project's direction. The tool visualizes the roadmap based on GitHub Projects, showing planned features and epics for upcoming releases.

### In-depth Release Analysis

For any given release, the tool provides deep-dive analytics by processing a wide range of data points and visualizing the relationships between them. This includes analyzing issue attributes (e.g., bug, feature, priority, and labels), development data such as associated branches and pull requests, and planning elements like milestones and their completion status.

By analyzing the ratios and connections between these elements, users can gain a much deeper understanding of the work involved in a release, identify potential risks, and track the overall health of the development process.

## System Architecture

The backend fetches data from external APIs, processes it, and stores it in a database. The backend then serves this data to the frontend, where it is visualized for the user.

Currently, the application primarily uses the **GitHub API** to retrieve data about the Frank!Framework's development. However, the architecture is designed to be extensible, meaning other external data sources can be integrated in a similar way in the future. This allows the application to be scaled with new integrations as needed.

![Insights-components-without-text](https://github.com/user-attachments/assets/278d03eb-d230-4246-93fe-705b0343dce6)
<p align="start">
  <em>A high-level overview of the data flow from external APIs to the user.</em>
</p>

## Quick Local Setup with Docker

For a fast and easy setup, you can use Docker Compose to run the entire application stack. This is the recommended method for most users.

1.  Ensure you have **Docker Desktop** installed, as it includes Docker and Docker Compose.
2.  Clone the repository:
    ```bash
    git clone [https://github.com/frankframework/insights.git](https://github.com/frankframework/insights.git)
    cd insights
    ```
3.  From the root of the project, run the following command. The `--build` flag forces a rebuild of the images to ensure you are running the latest version of the code, and the `-d` flag runs the containers in "detached mode" in the background.
    ```bash
    docker compose up -d --build
    ```

The application stack, including the frontend, backend, and database, is now running. You can access the frontend at `http://localhost:4200` and the backend API at `http://localhost:8080`.

## Manual Development Setup

For active development, a manual setup provides more granular control over the individual components.

### Prerequisites

For a manual setup, you will need Git, Java Development Kit (JDK 21), Node.js with the Angular CLI (`npm install -g @angular/cli`), and a running PostgreSQL database instance. You will also need an IDE for frontend work (like **WebStorm** or **VS Code**) and for the backend (like **IntelliJ IDEA**, **Eclipse**, or **VS Code**). A database tool like **pgAdmin** is optional but recommended for managing your database.

> **Note on Maven:** A separate installation of Apache Maven is not required. The backend project includes the Maven Wrapper (`mvnw`), which your IDE will automatically use to download dependencies and build the project.

### Steps

1.  **Clone the Repository**
    ```bash
    git clone [https://github.com/frankframework/insights.git](https://github.com/frankframework/insights.git)
    cd insights
    ```

2.  **Frontend Setup**
    Navigate to the frontend directory, install dependencies, and start the development server. `ng serve` hosts the frontend on `http://localhost:4200` and provides live reloading.
    ```bash
    cd InsightsFrontend
    npm install
    ng serve
    ```

3.  **Backend Setup**
    * **Open Project:** Open the `InsightsBackend` directory in your Java IDE. Your IDE will automatically detect it as a Maven project and use the wrapper to set it up.
    * **Activate Local Profile:** To use a local configuration, you must activate the `local` Spring profile. The easiest way to do this in **IntelliJ IDEA** is by adding the following to your Run Configuration's **VM options**:
        ```
        -Dspring.profiles.active=local
        ```
        This tells the application to load its settings from `application-local.properties`.

    * **Create & Configure Database:**
        * Create a new, empty database in your PostgreSQL instance.
        * Open the `src/main/resources/application-local.properties` file.
        * Update the datasource properties with your database URL, username, and password.
            ```properties
            spring.datasource.url=jdbc:postgresql://localhost:5432/your_database_name
            spring.datasource.username=your_username
            spring.datasource.password=your_password
            ```

    * **Configure GitHub API Access:**
        The application needs a GitHub token to fetch data from the Frank!Framework organization.
        * Create a **GitHub Personal Access Token (PAT)** with the necessary permissions (e.g., `read:org`, `project`). Follow the official guide: [Managing your personal access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens).
        * Add your GitHub Personal Access token and the GitHub Project ID to `application-local.properties`:
            ```properties
            github.token=YOUR_PERSONAL_ACCESS_TOKEN_HERE
            github.project.id=YOUR_GITHUB_PROJECT_ID_HERE
            ```

    * **Initial Data Injection:**
        To populate your database with data from GitHub for the first time, set the following property in `application-local.properties`:
        ```properties
        github.fetch=true
        ```
        After the first successful run, it's recommended for development purposes to set this to `false` to avoid refetching all data on every application start.

    * **Run Application:** Start the backend application directly from your IDE.

The application stack, including the frontend, backend, and database, is now running. You can access the frontend at http://localhost:4200 and the backend API at http://localhost:8080.

## Automated Workflows & Quality Assurance

The project uses a suite of automated workflows to ensure code quality, stability, and correctness. These are typically run in a CI/CD pipeline for every pull request and merge to the master branch.

**Linting**
Static code analysis is performed on both the backend and frontend to enforce consistent coding styles and catch common programming errors before they are merged.

**Unit & Integration Tests**
Automated tests are run to verify the functionality of individual components (unit tests) and their interactions (integration tests). This forms the core of our regression testing strategy.

**Build**
The workflow compiles the source code, runs tests, and packages the application into runnable artifacts (e.g., a JAR file for the backend and static assets for the frontend) to ensure it's always in a deployable state.

**End-to-End (E2E) Tests**
These tests simulate real user scenarios by running tests against a fully built and running application in a production-like environment. They verify that the integrated system works as expected from the user's perspective.

**Smoke Tests**
This workflow starts the full application with the latest version of the code (from a pull request branch or master). It then performs health checks on all core modules to confirm that the application starts up correctly as it should. A failed smoke test indicates a critical issue and typically triggers an immediate rollback.

**Stress Tests**
These tests push the system to its limits by simulating high traffic or data load. The goal is to measure performance, identify performance bottlenecks, and ensure the application remains stable and responsive under pressure. This test can be triggered manually to test the newest version of master.

## Contributing

This is an open-source project, and contributions are highly welcome! We follow the overarching contribution guidelines of the Frank!Framework organization. Before you start, please familiarize yourself with them.

**Code of Conduct**
All contributors are expected to adhere to our [Code of Conduct](https://github.com/frankframework/frankframework/blob/master/CODE_OF_CONDUCT.md).

**Contribution Guidelines**
For general guidelines like commit messages and pull request procedures, see the main [CONTRIBUTING.md](https://github.com/frankframework/frankframework/blob/master/CONTRIBUTING.md).

**Frontend Conventions**
All frontend code must adhere to the [Frank!Framework Frontend Conventions](https://github.com/frankframework/frontend-conventions).

**Backend Conventions**
New public classes and methods in the Java backend should be documented with **Javadoc**.

**Quality**
All new code must pass the existing tests and should be covered by new tests where applicable.

You can contribute by reporting bugs or suggesting new features by creating an issue, or by forking the repository and submitting a pull request with your improvements.

## License

This project is licensed under the Apache 2.0 License. See the `LICENSE` file for the full terms.
