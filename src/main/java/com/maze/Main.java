package com.maze;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maze.algos.MazeGenerator;
import com.maze.algos.MazeSolver;
import com.maze.database.Database;
import com.maze.models.Cell;
import com.maze.models.Maze;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Database database = new Database();

    public static void main(String[] args) {
        var app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
        }).start(7007);

        app.get("/", ctx -> ctx.redirect("/index.html"));

        // ─── Maze Generation ───────────────────────────────────────────────

        app.post("/api/generate", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            int rows = (Integer) req.get("rows");
            int cols = (Integer) req.get("cols");
            int braidPercent = req.containsKey("braidPercent")
                    ? ((Number) req.get("braidPercent")).intValue() : 0;

            MazeGenerator generator = new MazeGenerator();
            Maze maze = new Maze(rows, cols);
            generator.generate(maze);
            if (braidPercent > 0) {
                generator.makeBraid(maze, braidPercent);
            }
            ctx.json(serializeMaze(maze));
        });

        // ─── Algorithm Solver ──────────────────────────────────────────────

        app.post("/api/solve", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> mazeData = (Map<String, Object>) req.get("maze");
            String algoName = (String) req.getOrDefault("algoName", "astar");
            boolean animate = req.containsKey("animate") && Boolean.TRUE.equals(req.get("animate"));

            Maze maze = deserializeMaze(mazeData);
            Cell start = maze.getCell(0, 0);
            Cell end = maze.getCell(maze.getRows() - 1, maze.getCols() - 1);

            MazeSolver solver = new MazeSolver();
            long startTime = System.currentTimeMillis();

            List<Cell> solution;
            List<Cell> visited = new ArrayList<>();

            if (animate) {
                MazeSolver.SolveResult result;
                switch (algoName.toLowerCase()) {
                    case "dijkstra":
                        result = solver.solveDijkstraWithVisited(maze, start, end); break;
                    case "bfs":
                        result = solver.solveBFSWithVisited(maze, start, end); break;
                    case "dfs":
                        result = solver.solveDFSWithVisited(maze, start, end); break;
                    case "greedy":
                        result = solver.solveGreedyWithVisited(maze, start, end); break;
                    case "bidirectional":
                        result = solver.solveBidirectionalWithVisited(maze, start, end); break;
                    case "astar":
                    default:
                        result = solver.solveWithVisited(maze, start, end); break;
                }
                solution = result.solution;
                visited = result.visited;
            } else {
                switch (algoName.toLowerCase()) {
                    case "dijkstra":
                        solution = solver.solveDijkstra(maze, start, end); break;
                    case "bfs":
                        solution = solver.solveBFS(maze, start, end); break;
                    case "dfs":
                        solution = solver.solveDFS(maze, start, end); break;
                    case "greedy":
                        solution = solver.solveGreedy(maze, start, end); break;
                    case "bidirectional":
                        solution = solver.solveBidirectional(maze, start, end); break;
                    case "astar":
                    default:
                        solution = solver.solve(maze, start, end); break;
                }
            }

            long solveTime = System.currentTimeMillis() - startTime;

            for (Cell c : solution)
                c.setPath(true);

            Map<String, Object> response = new HashMap<>();
            response.put("solution", serializePath(solution));
            response.put("solveTime", solveTime);
            response.put("pathLength", solution.size());
            if (animate) {
                response.put("visited", serializePath(visited));
            }
            ctx.json(response);
        });

        // ─── ALGO_RUN — submit & leaderboard ──────────────────────────────

        app.post("/api/scores", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            String playerName = (String) req.get("playerName");
            String mazeSize = (String) req.get("mazeSize");
            String algoName = (String) req.getOrDefault("algoName", "astar");
            long solveTime = ((Number) req.get("solveTime")).longValue();
            int pathLength = req.containsKey("pathLength") ? ((Number) req.get("pathLength")).intValue() : 0;
            Integer mazeId = req.get("mazeId") != null ? ((Number) req.get("mazeId")).intValue() : null;

            int playerId = database.getOrCreatePlayer(playerName);
            database.addAlgoRun(playerId, mazeId, mazeSize, algoName, solveTime, pathLength);

            ctx.json(Map.of("success", true));
        });

        app.get("/api/scores/top", ctx -> {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(5);
            ctx.json(database.getTopAlgoRuns(limit));
        });

        // ─── MANUAL — submit & leaderboard ────────────────────────────────

        app.post("/api/attempts", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            String playerName = (String) req.get("playerName");
            String mazeSize = (String) req.get("mazeSize");
            long duration = ((Number) req.get("duration")).longValue();
            int steps = ((Number) req.get("steps")).intValue();
            Integer mazeId = req.get("mazeId") != null ? ((Number) req.get("mazeId")).intValue() : null;

            int playerId = database.getOrCreatePlayer(playerName);
            database.addManual(playerId, mazeId, mazeSize, duration, steps);

            ctx.json(Map.of("success", true));
        });

        app.get("/api/attempts/top", ctx -> {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(5);
            ctx.json(database.getTopManual(limit));
        });

        // ─── SAVES + MAZE ──────────────────────────────────────────────────
        // NOTE: /api/mazes/saved must be declared before /api/mazes/{id}

        app.post("/api/mazes/save", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            String playerName = (String) req.get("playerName");
            String label = (String) req.getOrDefault("label", "Untitled Maze");

            @SuppressWarnings("unchecked")
            Map<String, Object> mazeData = (Map<String, Object>) req.get("mazeData");
            int rows = ((Number) mazeData.get("rows")).intValue();
            int cols = ((Number) mazeData.get("cols")).intValue();
            String gridJson = mapper.writeValueAsString(mazeData);

            int mazeId = database.insertMaze(rows, cols, gridJson);
            int playerId = database.getOrCreatePlayer(playerName);
            database.addSave(playerId, mazeId, label);

            ctx.json(Map.of("success", true, "mazeId", mazeId));
        });

        app.get("/api/mazes/saved", ctx -> {
            ctx.json(database.getSavedMazes());
        });

        app.get("/api/mazes/{id}", ctx -> {
            int mazeId = Integer.parseInt(ctx.pathParam("id"));
            String gridJson = database.getMazeById(mazeId);

            if (gridJson == null) {
                ctx.status(404).json(Map.of("error", "Maze not found"));
                return;
            }
            ctx.json(mapper.readValue(gridJson, Map.class));
        });

        // ─── Shutdown ──────────────────────────────────────────────────────

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            database.close();
            System.out.println("Database connection closed.");
        }));

        System.out.println("🧩 Maze Master running on http://localhost:7007");
    }

    // ─── Serialisation helpers ─────────────────────────────────────────────

    private static Map<String, Object> serializeMaze(Maze maze) {
        Map<String, Object> data = new HashMap<>();
        data.put("rows", maze.getRows());
        data.put("cols", maze.getCols());

        List<Map<String, Object>> cells = new ArrayList<>();
        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getCols(); c++) {
                Cell cell = maze.getCell(r, c);
                Map<String, Object> cd = new HashMap<>();
                cd.put("row", cell.getRow());
                cd.put("col", cell.getCol());
                cd.put("top", cell.hasTopWall());
                cd.put("right", cell.hasRightWall());
                cd.put("bottom", cell.hasBottomWall());
                cd.put("left", cell.hasLeftWall());
                cd.put("isPath", cell.isPath());
                cells.add(cd);
            }
        }
        data.put("cells", cells);
        return data;
    }

    private static Maze deserializeMaze(Map<String, Object> data) {
        int rows = ((Number) data.get("rows")).intValue();
        int cols = ((Number) data.get("cols")).intValue();
        Maze maze = new Maze(rows, cols);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) data.get("cells");
        for (Map<String, Object> cd : cells) {
            int r = ((Number) cd.get("row")).intValue();
            int c = ((Number) cd.get("col")).intValue();
            Cell cell = maze.getCell(r, c);
            cell.setTopWall((Boolean) cd.get("top"));
            cell.setRightWall((Boolean) cd.get("right"));
            cell.setBottomWall((Boolean) cd.get("bottom"));
            cell.setLeftWall((Boolean) cd.get("left"));
        }
        return maze;
    }

    private static List<Map<String, Integer>> serializePath(List<Cell> path) {
        List<Map<String, Integer>> out = new ArrayList<>();
        for (Cell cell : path) {
            Map<String, Integer> coord = new HashMap<>();
            coord.put("row", cell.getRow());
            coord.put("col", cell.getCol());
            out.add(coord);
        }
        return out;
    }
}
