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

            Maze maze = new Maze(rows, cols);
            new MazeGenerator().generate(maze);
            ctx.json(serializeMaze(maze));
        });

        // ─── Algorithm Solver ──────────────────────────────────────────────

        app.post("/api/solve", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> mazeData = (Map<String, Object>) req.get("maze");
            String algorithm = (String) req.getOrDefault("algorithm", "astar");

            Maze maze = deserializeMaze(mazeData);
            MazeSolver solver = new MazeSolver();
            Cell start = maze.getCell(0, 0);
            Cell end   = maze.getCell(maze.getRows() - 1, maze.getCols() - 1);

            long startTime = System.currentTimeMillis();
            List<Cell> solution = "dijkstra".equalsIgnoreCase(algorithm)
                    ? solver.solveDijkstra(maze, start, end)
                    : solver.solve(maze, start, end);
            long solveTime = System.currentTimeMillis() - startTime;

            for (Cell c : solution) c.setPath(true);

            Map<String, Object> response = new HashMap<>();
            response.put("solution",    serializePath(solution));
            response.put("solveTimeMs", solveTime);
            response.put("pathLength",  solution.size());
            ctx.json(response);
        });

        // ─── Algorithm Run (Score) ─────────────────────────────────────────

        app.post("/api/scores", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            String  playerName  = (String)  req.get("playerName");
            String  mazeSize    = (String)  req.get("mazeSize");
            String  algorithm   = (String)  req.getOrDefault("algorithm", "astar");
            long    solveTimeMs = ((Number) req.get("solveTimeMs")).longValue();
            int     pathLength  = req.containsKey("pathLength") ? ((Number) req.get("pathLength")).intValue() : 0;
            Integer mazeId      = req.get("mazeId") != null ? ((Number) req.get("mazeId")).intValue() : null;

            int playerId = database.getOrCreatePlayer(playerName);
            database.addAlgorithmRun(playerId, mazeId, mazeSize, algorithm, solveTimeMs, pathLength);

            ctx.json(Map.of("success", true));
        });

        app.get("/api/scores/top", ctx -> {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(5);
            ctx.json(database.getTopAlgorithmRuns(limit));
        });

        // ─── Manual Attempt ────────────────────────────────────────────────

        app.post("/api/attempts", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            String  playerName = (String)  req.get("playerName");
            String  mazeSize   = (String)  req.get("mazeSize");
            long    durationMs = ((Number) req.get("durationMs")).longValue();
            int     steps      = ((Number) req.get("steps")).intValue();
            Integer mazeId     = req.get("mazeId") != null ? ((Number) req.get("mazeId")).intValue() : null;

            int playerId = database.getOrCreatePlayer(playerName);
            database.addManualAttempt(playerId, mazeId, mazeSize, durationMs, steps);

            ctx.json(Map.of("success", true));
        });

        app.get("/api/attempts/top", ctx -> {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(5);
            ctx.json(database.getTopManualAttempts(limit));
        });

        // ─── Saved Mazes ───────────────────────────────────────────────────
        // NOTE: /api/mazes/saved MUST be registered before /api/mazes/{id}
        // so Javalin does not treat the literal "saved" as an id parameter.

        app.post("/api/mazes/save", ctx -> {
            Map<String, Object> req = mapper.readValue(ctx.body(), Map.class);
            String playerName = (String) req.get("playerName");
            String label      = (String) req.getOrDefault("label", "Untitled Maze");

            @SuppressWarnings("unchecked")
            Map<String, Object> mazeData = (Map<String, Object>) req.get("mazeData");
            int    rows     = ((Number) mazeData.get("rows")).intValue();
            int    cols     = ((Number) mazeData.get("cols")).intValue();
            String gridJson = mapper.writeValueAsString(mazeData);

            int playerId = database.getOrCreatePlayer(playerName);
            int mazeId   = database.saveMaze(playerId, rows, cols, gridJson, label);

            ctx.json(Map.of("success", true, "mazeId", mazeId));
        });

        app.get("/api/mazes/saved", ctx -> {
            ctx.json(database.getSavedMazes());
        });

        app.get("/api/mazes/{id}", ctx -> {
            int    id       = Integer.parseInt(ctx.pathParam("id"));
            String gridJson = database.getMazeById(id);

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
                cd.put("row",    cell.getRow());
                cd.put("col",    cell.getCol());
                cd.put("top",    cell.hasTopWall());
                cd.put("right",  cell.hasRightWall());
                cd.put("bottom", cell.hasBottomWall());
                cd.put("left",   cell.hasLeftWall());
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
