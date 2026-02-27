package com.maze;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    var app = Javalin.create(config -> {
      // Serve static files from resources/static
      config.staticFiles.add("/static", Location.CLASSPATH);
    }).start(7007);

    // API Endpoint: Simulates a solved maze being sent back to the browser
    app.get("/api/solve", ctx -> {
      int rows = 20;
      int cols = 20;
      int[][] bigMaze = new int[rows][cols];

      // Simple pattern for the prototype: Fill with some walls
      for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
          // Create a border and some random internal walls
          if (i == 0 || i == rows - 1 || j == 0 || j == cols - 1 || (i % 2 == 0 && j % 2 == 0)) {
            bigMaze[i][j] = 1; // Wall
          } else {
            bigMaze[i][j] = 0; // Path
          }
        }
      }
      ctx.json(Map.of("maze", bigMaze, "rows", rows, "cols", cols));
    });
    // Database Trigger: Simulates saving a highscore
    app.post("/api/save", ctx -> {
      System.out.println("DB Triggered: Saving highscore to SQLite...");
      ctx.status(201).result("Score saved to DB!");
    });
  }
}
