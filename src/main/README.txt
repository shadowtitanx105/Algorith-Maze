Maze Master: Full-Stack Solver

A cross-platform Java web application that generates, visualizes, and solves mazes using various algorithms. This project utilizes a Classic 3-Tier Architecture to bridge a high-performance Java backend with a dynamic HTML5 Canvas frontend.

Technical Architecture & Flow:

The application flow follows a strict request-response cycle:

Presentation Layer (Frontend): * HTML5 & CSS3: Defines the UI structure and layout.

    JavaScript (Canvas API): Interacts with the browser's graphics engine to "draw" the maze based on data received from the server.

Application Layer (Backend):

    Java 17: Handles the heavy lifting of maze generation and pathfinding logic.

    Javalin: A lightweight web framework that serves static files and provides API endpoints (REST).

    Jackson: Automatically serializes Java 2D arrays into JSON for the frontend.

Data Layer (Persistence):

    SQLite: A serverless, file-based database used to store high scores and saved mazes. Utilises a six-entity Schema normmalised upto 3NF.
