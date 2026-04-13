# Java Usage Report — Algorith-Maze

> **Project:** `com.maze` / `maze-master`
> **Java Version:** 17 (LTS)
> **Build Tool:** Maven (with Maven Wrapper)
> **Entry Point:** `com.maze.Main`

---

## 1. Project Structure Overview

```
src/main/java/com/maze/
├── Main.java                   ← HTTP server + REST API + serialisation
├── algos/
│   ├── MazeGenerator.java      ← Maze generation algorithm (DFS)
│   └── MazeSolver.java         ← Solving algorithms (A*, Dijkstra)
├── database/
│   └── Database.java           ← SQLite persistence layer (JDBC)
└── models/
    ├── Cell.java               ← Single cell domain model
    └── Maze.java               ← Maze grid domain model
```

Java is the **entire backend** of this application. The frontend is static HTML/CSS/JavaScript served by the Java server. All business logic, persistence, and HTTP routing are handled in Java.

---

## 2. Build System — Maven (`pom.xml`)

The project uses **Maven** with a Maven Wrapper (`mvnw`) so no global Maven installation is required.

| Config | Value |
|---|---|
| Group ID | `com.maze` |
| Artifact ID | `maze-master` |
| Java Source/Target | `17` |
| Source Encoding | `UTF-8` |
| Main class | `com.maze.Main` |

### Dependencies

| Library | Version | Role |
|---|---|---|
| `io.javalin:javalin` | `6.1.3` | Embedded HTTP server & REST routing |
| `com.fasterxml.jackson.core:jackson-databind` | `2.17.0` | JSON serialisation / deserialisation |
| `org.slf4j:slf4j-simple` | `2.0.12` | Logging backend (required by Javalin) |
| `org.xerial:sqlite-jdbc` | `3.45.2.0` | SQLite database driver (JDBC) |

The `exec-maven-plugin` is used to run `com.maze.Main` directly via `mvn exec:java`.

---

## 3. Domain Models (`com.maze.models`)

### 3.1 `Cell.java`
Represents a **single cell** in the maze grid.

**Fields:**
- `final int row, col` — immutable position
- `boolean topWall, rightWall, bottomWall, leftWall` — wall state on all 4 sides (all `true` by default)
- `boolean visited` — used during generation (DFS backtracking)
- `boolean isPath` — marks cells that are part of the solved path

**Key method — `removeWallBetween(Cell neighbor)`:**
Uses row/column arithmetic to determine the shared wall direction and knocks down the wall on both sides:

```java
if (rowDiff == 1) {       // neighbor is above
    this.topWall = false;
    neighbor.bottomWall = false;
} else if (colDiff == -1) { // neighbor is to the right
    this.rightWall = false;
    neighbor.leftWall = false;
}
```
This bi-directional mutation is the core of how passages are carved during generation.

---

### 3.2 `Maze.java`
Holds the **full grid** as a 2D array `Cell[][] grid`.

**Key responsibilities:**
- `initializeGrid()` — populates every cell in the grid on construction
- `getUnvisitedNeighbors(Cell)` — returns the 4 cardinal neighbours that haven't been visited yet, used by the **generator**
- `getAccessibleNeighbors(Cell)` — returns only neighbours reachable through open walls, used by the **solvers**
- `resetVisited()` / `resetPath()` — bulk-resets cell state after generation or solving

The distinction between `getUnvisitedNeighbors` (wall-agnostic, visits all 4 directions) and `getAccessibleNeighbors` (wall-aware) is the key design decision separating generation logic from solving logic.

---

## 4. Algorithm Layer (`com.maze.algos`)

### 4.1 `MazeGenerator.java` — Recursive Backtracker (Iterative DFS)

Uses Java's `java.util.Stack<Cell>` to implement an **iterative depth-first search** maze generation algorithm (avoids stack overflow from true recursion on large grids).

**Algorithm:**
1. Start at cell `(0,0)`, push to stack, mark visited.
2. While stack is non-empty:
   - Peek the top cell.
   - If it has unvisited neighbours, pick one **randomly** (`Random.nextInt()`), remove the wall between them, push the new cell.
   - If no unvisited neighbours, **backtrack** by popping the stack.
3. Call `maze.resetVisited()` — cleans up the visited flags post-generation.

**Two generation modes:**
- `generate(Maze maze)` — uses an instance-level `Random` (seeded at construction time).
- `generateWithSeed(Maze maze, long seed)` — creates a local `Random(seed)` for **deterministic, reproducible** mazes.

---

### 4.2 `MazeSolver.java` — A* and Dijkstra

Contains a **private inner class `Node`** implementing `Comparable<Node>` for use in a `PriorityQueue`:

```java
private static class Node implements Comparable<Node> {
    Cell cell;
    Node parent;    // for path reconstruction
    double gCost;   // cost from start
    double hCost;   // heuristic estimate to end

    double fCost() { return gCost + hCost; }

    @Override
    public int compareTo(Node other) {
        return Double.compare(this.fCost(), other.fCost());
    }
}
```

#### A\* (`solve()`)
- Uses `PriorityQueue<Node>` (min-heap on `fCost`) + `HashSet<Cell>` closed set + `HashMap<Cell, Double>` for g-scores.
- **Heuristic:** Manhattan distance: `|row_a - row_b| + |col_a - col_b|`
- Explores cells in order of `f = g + h`, guaranteeing the **shortest path**.

#### Dijkstra (`solveDijkstra()`)
- Same structure as A*, but `hCost` is always `0` — degenerates A* into Dijkstra.
- Uses an extra `Map<Cell, Node> nodes` to track the best known node per cell.
- Guaranteed shortest path with no heuristic assumption.

**Path Reconstruction (`reconstructPath`):**
Walks the `parent` chain from the end node back to the start, inserting at index 0 (`path.add(0, current.cell)`) to return the path in forward order.

---

## 5. Database Layer (`com.maze.database.Database`)

The largest Java file (491 lines). Manages a **SQLite database** via raw JDBC (`java.sql.*`).

### 5.1 Schema — 6 Tables

The schema is modelled as a **relational database** with strong entities, weak entities, and associative entities:

| Table | Type | Description |
|---|---|---|
| `player` | Strong entity | Stores player names; auto-incremented PK |
| `maze` | Strong entity | Stores serialised grid JSON; independent of player |
| `saves` | **Associative entity** (M:N) | Links players to mazes they've saved; composite PK `(player_id, maze_id)` |
| `algo_run` | Strong entity | Records each algorithm solve with time & path length |
| `manual` | Strong entity | Records each manual player attempt with steps & duration |
| `persobest` | **Weak entity** (of `player`) | Tracks personal bests; composite PK `(player_id, maze_size)` as partial key |

**Performance indexes:**
```sql
CREATE INDEX idx_algo_time    ON algo_run(solve_time ASC)
CREATE INDEX idx_manual_steps ON manual(steps ASC)
CREATE INDEX idx_saves_maze   ON saves(maze_id)
```

### 5.2 Migration System

On startup, `migrate()` checks for **old schema tables** (`algorithm_run`, `manual_attempt`, `personal_best`) and migrates data into the new schema using `PreparedStatement`s, then drops the old tables. It also handles the case where `maze` had a `player_id` column (old design) and moves those associations into the `saves` table.

### 5.3 JDBC Patterns Used

- **`DriverManager.getConnection(DB_URL)`** — establishes the connection.
- **`PreparedStatement`** — used for all parameterised queries to prevent SQL injection.
- **`Statement.RETURN_GENERATED_KEYS`** — used in `insertMaze()` to retrieve the auto-generated `maze_id`.
- **`ResultSet`** — iterated to build `List<Map<String, Object>>` results returned as JSON.
- **Try-with-resources** (`try (PreparedStatement p = ...)`) — ensures statements are always closed.
- **`Types.INTEGER` + `p.setNull()`** — correctly handles nullable `maze_id` foreign key.
- **`PRAGMA foreign_keys = ON`** — enables SQLite foreign key enforcement (off by default).
- **`LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)`** — timestamps in ISO 8601 format.

### 5.4 Key Data Operations

| Method | SQL Pattern |
|---|---|
| `getOrCreatePlayer` | `INSERT OR IGNORE` then `SELECT` |
| `insertMaze` | `INSERT` + `RETURN_GENERATED_KEYS` |
| `addSave` | `INSERT OR REPLACE` (upsert) |
| `getSavedMazes` | 3-table `JOIN` (saves ⋈ maze ⋈ player) |
| `getTopAlgoRuns` | `JOIN` + `ORDER BY solve_time ASC LIMIT ?` |
| `getTopManual` | `JOIN` + `ORDER BY steps ASC, duration ASC LIMIT ?` |
| `upsertPersoBest` | `SELECT` then conditional `INSERT` or `UPDATE` |

---

## 6. HTTP Server & REST API (`com.maze.Main`)

Uses **Javalin 6** (built on Jetty) as an embedded HTTP server — no application server (Tomcat/JBoss) is needed.

```java
var app = Javalin.create(config -> {
    config.staticFiles.add("/static", Location.CLASSPATH);
}).start(7007);
```

Static files (`index.html`, CSS, JS) are served directly from the classpath (`src/main/resources/static`).

### REST Endpoints

| Method | Path | Handler Summary |
|---|---|---|
| `GET` | `/` | Redirect to `/index.html` |
| `POST` | `/api/generate` | Generate a new maze of given rows/cols |
| `POST` | `/api/solve` | Solve a maze with A* or Dijkstra |
| `POST` | `/api/scores` | Submit an algorithm solve score |
| `GET` | `/api/scores/top` | Get top N algorithm scores |
| `POST` | `/api/attempts` | Submit a manual play attempt |
| `GET` | `/api/attempts/top` | Get top N manual attempts |
| `POST` | `/api/mazes/save` | Save a maze for a player |
| `GET` | `/api/mazes/saved` | List all saved mazes |
| `GET` | `/api/mazes/{id}` | Load a specific maze by ID |

> [!IMPORTANT]
> `/api/mazes/saved` must be declared **before** `/api/mazes/{id}` in the routing setup, otherwise Javalin would match `saved` as the `{id}` path parameter. This is explicitly noted in a comment in `Main.java`.

### JSON Handling

**Jackson `ObjectMapper`** is used as a singleton static field:
```java
private static final ObjectMapper mapper = new ObjectMapper();
```
- `mapper.readValue(ctx.body(), Map.class)` — deserialises request body to a generic `Map`.
- `ctx.json(...)` — Javalin serialises response objects to JSON automatically (using Jackson internally).
- `mapper.writeValueAsString(mazeData)` — converts the maze map to a JSON string for storage in SQLite.
- `mapper.readValue(gridJson, Map.class)` — deserialises the stored JSON back to a `Map` for loading.

### Serialisation Helpers

Three private static methods handle the conversion between `Maze`/`Cell` domain objects and plain `Map<String, Object>` structures (which Jackson can serialise to JSON):

- **`serializeMaze(Maze)`** — iterates all cells, extracts wall booleans and position, returns a flat `List<Map>`.
- **`deserializeMaze(Map)`** — rebuilds a `Maze` object from a JSON-sourced map, setting wall states on each cell.
- **`serializePath(List<Cell>)`** — converts a solution path (list of cells) to a list of `{row, col}` coordinate maps.

---

## 7. Application Lifecycle

### Startup Sequence
1. `Javalin.create()` — configure static file serving.
2. `.start(7007)` — bind and listen.
3. `new Database()` — connect to SQLite, run `initializeDatabase()`, run `migrate()`.
4. All route handlers are registered with lambda expressions.

### Graceful Shutdown
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    database.close();
    System.out.println("Database connection closed.");
}));
```
A JVM **shutdown hook** ensures the SQLite JDBC connection is properly closed when the JVM exits (e.g. `Ctrl+C`).

---

## 8. Java Language Features Used

| Feature | Java Version | Where Used |
|---|---|---|
| `var` local type inference | Java 10+ | `var app = Javalin.create(...)` |
| Lambda expressions | Java 8+ | All Javalin route handlers, shutdown hook |
| Text blocks (`"""..."""`) | Java 15+ | SQL strings in `Database.java` |
| `Map.of(...)` | Java 9+ | Inline response maps in route handlers |
| Try-with-resources | Java 7+ | All JDBC `Statement`/`ResultSet` usage |
| `Comparable<T>` | Core | `Node` in `MazeSolver` for `PriorityQueue` ordering |
| `@SuppressWarnings` | Core | Suppressing unchecked cast warnings on raw `Map` deserialisation |
| Private inner class | Core | `Node` inside `MazeSolver` |

---

## 9. Data Flow Summary

```
Browser (JS)
    │  HTTP POST /api/generate  {rows, cols}
    ▼
Main.java (Javalin route handler)
    │  mapper.readValue() → Map
    ├─ new Maze(rows, cols)      ← Maze.java initialises Cell[][] grid
    ├─ new MazeGenerator().generate(maze)
    │       └─ Stack<Cell> DFS, random.nextInt(), cell.removeWallBetween()
    └─ serializeMaze(maze) → Map → ctx.json()
    │  HTTP response: {rows, cols, cells:[...]}
    ▼
Browser renders maze

Browser (JS)
    │  HTTP POST /api/solve  {maze:{...}, algoName:"astar"}
    ▼
Main.java
    ├─ deserializeMaze(mazeData) → Maze object
    ├─ solver.solve() / solver.solveDijkstra()
    │       └─ PriorityQueue<Node>, HashSet<Cell>, HashMap<Cell,Double>
    │          Manhattan heuristic, path reconstruction via parent chain
    └─ serializePath(solution) → ctx.json()

Browser (JS)
    │  HTTP POST /api/mazes/save  {playerName, label, mazeData}
    ▼
Main.java
    ├─ mapper.writeValueAsString(mazeData) → gridJson (String)
    ├─ database.insertMaze(rows, cols, gridJson) → mazeId  (JDBC INSERT + GENERATED_KEYS)
    ├─ database.getOrCreatePlayer(playerName) → playerId   (JDBC INSERT OR IGNORE + SELECT)
    └─ database.addSave(playerId, mazeId, label)           (JDBC INSERT OR REPLACE)
```

---

## 10. Summary Statistics

| Metric | Count |
|---|---|
| Java source files | **6** |
| Total Java lines of code | **~1,107** |
| REST API endpoints | **10** |
| Database tables | **6** |
| Algorithms implemented | **2** (A*, Dijkstra) |
| External Java libraries | **4** |
| Java version | **17** |
