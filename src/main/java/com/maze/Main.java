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
      int[][] solvedMaze = {
          { 0, 1, 0 },
          { 0, 0, 0 },
          { 1, 1, 0 }
      };
      ctx.json(Map.of("maze", solvedMaze, "status", "Solved by Java"));
    });

    // Database Trigger: Simulates saving a highscore
    app.post("/api/save", ctx -> {
      System.out.println("DB Triggered: Saving highscore to SQLite...");
      ctx.status(201).result("Score saved to DB!");
    });
  }
}
