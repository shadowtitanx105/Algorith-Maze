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

    // ─── Maze Generation ───────────────────────────────────────────────────────

    app.post("/api/generate", ctx -> {
      Map<String, Object> request = mapper.readValue(ctx.body(), Map.class);
      int rows = (Integer) request.get("rows");
      int cols = (Integer) request.get("cols");

      Maze maze = new Maze(rows, cols);
      MazeGenerator generator = new MazeGenerator();
      generator.generate(maze);

      Map<String, Object> response = serializeMaze(maze);
      ctx.json(response);
    });

    // ─── Algorithm Solver ──────────────────────────────────────────────────────

    app.post("/api/solve", ctx -> {
      Map<String, Object> request = mapper.readValue(ctx.body(), Map.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> mazeData = (Map<String, Object>) request.get("maze");
      String algorithm = (String) request.getOrDefault("algorithm", "astar");

      Maze maze = deserializeMaze(mazeData);
      MazeSolver solver = new MazeSolver();

      Cell start = maze.getCell(0, 0);
      Cell end = maze.getCell(maze.getRows() - 1, maze.getCols() - 1);

      long startTime = System.currentTimeMillis();
      List<Cell> solution;

      if ("dijkstra".equalsIgnoreCase(algorithm)) {
        solution = solver.solveDijkstra(maze, start, end);
      } else {
        solution = solver.solve(maze, start, end);
      }

      long solveTime = System.currentTimeMillis() - startTime;

      for (Cell cell : solution) {
        cell.setPath(true);
      }

      Map<String, Object> response = new HashMap<>();
      response.put("solution", serializePath(solution));
      response.put("solveTimeMs", solveTime);
      response.put("pathLength", solution.size());

      ctx.json(response);
    });

    // ─── Algorithm High Scores ─────────────────────────────────────────────────

    app.post("/api/scores", ctx -> {
      Map<String, Object> request = mapper.readValue(ctx.body(), Map.class);
      String playerName = (String) request.get("playerName");
      String mazeSize = (String) request.get("mazeSize");
      Number solveTimeMsNum = (Number) request.get("solveTimeMs");
      long solveTimeMs = solveTimeMsNum.longValue();
      String algorithm = (String) request.getOrDefault("algorithm", "astar");

      database.addHighScore(playerName, mazeSize, solveTimeMs, algorithm);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      ctx.json(response);
    });

    app.get("/api/scores/top", ctx -> {
      int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);
      List<Map<String, Object>> scores = database.getTopScores(limit);
      ctx.json(scores);
    });

    app.get("/api/scores/size/{size}", ctx -> {
      String size = ctx.pathParam("size");
      int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);
      List<Map<String, Object>> scores = database.getScoresBySize(size, limit);
      ctx.json(scores);
    });

    // ─── Player Attempts ───────────────────────────────────────────────────────

    app.post("/api/attempts", ctx -> {
      Map<String, Object> request = mapper.readValue(ctx.body(), Map.class);
      String playerName = (String) request.get("playerName");
      String mazeSize   = (String) request.get("mazeSize");
      Number durationNum = (Number) request.get("durationMs");
      Number stepsNum    = (Number) request.get("steps");
      long durationMs = durationNum.longValue();
      int  steps      = stepsNum.intValue();

      database.addAttempt(playerName, mazeSize, durationMs, steps, true);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      ctx.json(response);
    });

    app.get("/api/attempts/top", ctx -> {
      int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(5);
      List<Map<String, Object>> attempts = database.getTopAttempts(limit);
      ctx.json(attempts);
    });

    // ─── Shutdown ──────────────────────────────────────────────────────────────

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      database.close();
      System.out.println("Database connection closed");
    }));

    System.out.println("🧩 Maze Master server running on http://localhost:7007");
  }

  private static Map<String, Object> serializeMaze(Maze maze) {
    Map<String, Object> data = new HashMap<>();
    data.put("rows", maze.getRows());
    data.put("cols", maze.getCols());

    List<Map<String, Object>> cells = new ArrayList<>();
    for (int r = 0; r < maze.getRows(); r++) {
      for (int c = 0; c < maze.getCols(); c++) {
        Cell cell = maze.getCell(r, c);
        Map<String, Object> cellData = new HashMap<>();
        cellData.put("row", cell.getRow());
        cellData.put("col", cell.getCol());
        cellData.put("top", cell.hasTopWall());
        cellData.put("right", cell.hasRightWall());
        cellData.put("bottom", cell.hasBottomWall());
        cellData.put("left", cell.hasLeftWall());
        cellData.put("isPath", cell.isPath());
        cells.add(cellData);
      }
    }
    data.put("cells", cells);
    return data;
  }

  private static Maze deserializeMaze(Map<String, Object> data) {
    Number rowsNum = (Number) data.get("rows");
    Number colsNum = (Number) data.get("cols");
    int rows = rowsNum.intValue();
    int cols = colsNum.intValue();

    Maze maze = new Maze(rows, cols);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> cells = (List<Map<String, Object>>) data.get("cells");

    for (Map<String, Object> cellData : cells) {
      Number rNum = (Number) cellData.get("row");
      Number cNum = (Number) cellData.get("col");
      int r = rNum.intValue();
      int c = cNum.intValue();
      Cell cell = maze.getCell(r, c);

      cell.setTopWall((Boolean) cellData.get("top"));
      cell.setRightWall((Boolean) cellData.get("right"));
      cell.setBottomWall((Boolean) cellData.get("bottom"));
      cell.setLeftWall((Boolean) cellData.get("left"));
    }

    return maze;
  }

  private static List<Map<String, Integer>> serializePath(List<Cell> path) {
    List<Map<String, Integer>> serialized = new ArrayList<>();
    for (Cell cell : path) {
      Map<String, Integer> coord = new HashMap<>();
      coord.put("row", cell.getRow());
      coord.put("col", cell.getCol());
      serialized.add(coord);
    }
    return serialized;
  }
}
