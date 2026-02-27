Maze Master: Full-Stack Solver

A cross-platform Java web application that generates, visualizes, and solves mazes using various algorithms. This project utilizes a Classic 3-Tier Architecture to bridge a high-performance Java backend with a dynamic HTML5 Canvas frontend.

Technical Architecture & Flow

The application flow follows a strict request-response cycle:

    Presentation Layer (Frontend): * HTML5 & CSS3: Defines the UI structure and layout.

        JavaScript (Canvas API): Interacts with the browser's graphics engine to "draw" the maze based on data received from the server.

    Application Layer (Backend):

        Java 17: Handles the heavy lifting of maze generation and pathfinding logic.

        Javalin: A lightweight web framework that serves static files and provides API endpoints (REST).

        Jackson: Automatically serializes Java 2D arrays into JSON for the frontend.

    Data Layer (Persistence):

        SQLite: A serverless, file-based database used to store high scores and saved mazes without requiring a complex setup for team members.

Execution Instructions

This project uses the Maven Wrapper (mvnw), ensuring that all team members use the exact same Maven version and dependencies regardless of their OS.

For Linux (Arch / macOS)

    Grant Permissions: Ensure the wrapper script is executable (only needs to be done once).
    Bash

    chmod +x mvnw

    Run the App: Use the wrapper to compile and start the server.
    Bash

    ./mvnw compile exec:java

    Access: Open your browser to http://localhost:7007.

For Windows (PowerShell / CMD)

    Open Terminal: Open PowerShell or Command Prompt in the project root directory.

    Run the App: Use the Windows-specific batch script.
    PowerShell

    .\mvnw.cmd compile exec:java

    Access: Open your browser to http://localhost:7007.

Project Structure
.
├── mvnw / mvnw.cmd        # Cross-platform entry points
├── pom.xml                # Project dependencies (Javalin, SQLite, Jackson)
└── src
    └── main
        ├── java/com/maze
        │   ├── algos/      # Pathfinding & Generation algorithms
        │   ├── database/   # JDBC & SQL logic
        │   └── Main.java   # Server entry point & API routes
        └── resources
            ├── db/         # SQL migration scripts
            └── static/     # HTML, CSS, and JS (Frontend)

Developer Notes

    Dependencies: All libraries are managed via pom.xml. If you add a new library, run the compile command again to download it.

    Port: The server defaults to port 7007. Ensure this port is not being used by another application.

    Git: Always run git pull before starting work to sync the latest algorithms and frontend changes from the team.

Termination of Program:
  To terminate the program, press ctrl+c in the terminal where the program was launched from  
