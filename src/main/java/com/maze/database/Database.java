package com.maze.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:maze_master.db";
    private Connection connection;

    public Database() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private void initializeDatabase() {
        String createHighScoresSQL = """
            CREATE TABLE IF NOT EXISTS high_scores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_name TEXT NOT NULL,
                maze_size TEXT NOT NULL,
                solve_time_ms INTEGER NOT NULL,
                algorithm TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
        """;

        String createAttemptsSQL = """
            CREATE TABLE IF NOT EXISTS player_attempts (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                player_name TEXT    NOT NULL,
                maze_size   TEXT    NOT NULL,
                duration_ms INTEGER NOT NULL,
                steps       INTEGER NOT NULL,
                completed   INTEGER NOT NULL,
                created_at  TEXT    NOT NULL
            )
        """;

        String createAttemptsIdxSQL = """
            CREATE INDEX IF NOT EXISTS idx_attempts_steps ON player_attempts(steps ASC)
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createHighScoresSQL);
            stmt.execute(createAttemptsSQL);
            stmt.execute(createAttemptsIdxSQL);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    // ─── High Scores ───────────────────────────────────────────────────────────

    public void addHighScore(String playerName, String mazeSize, long solveTimeMs, String algorithm) {
        String insertSQL = """
            INSERT INTO high_scores (player_name, maze_size, solve_time_ms, algorithm, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, playerName);
            pstmt.setString(2, mazeSize);
            pstmt.setLong(3, solveTimeMs);
            pstmt.setString(4, algorithm);
            pstmt.setString(5, timestamp);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to add high score: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getTopScores(int limit) {
        String querySQL = """
            SELECT player_name, maze_size, solve_time_ms, algorithm, created_at
            FROM high_scores
            ORDER BY solve_time_ms ASC
            LIMIT ?
        """;

        List<Map<String, Object>> scores = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> score = new HashMap<>();
                score.put("playerName", rs.getString("player_name"));
                score.put("mazeSize", rs.getString("maze_size"));
                score.put("solveTimeMs", rs.getLong("solve_time_ms"));
                score.put("algorithm", rs.getString("algorithm"));
                score.put("createdAt", rs.getString("created_at"));
                scores.add(score);
            }
        } catch (SQLException e) {
            System.err.println("Failed to retrieve scores: " + e.getMessage());
        }

        return scores;
    }

    public List<Map<String, Object>> getScoresBySize(String mazeSize, int limit) {
        String querySQL = """
            SELECT player_name, maze_size, solve_time_ms, algorithm, created_at
            FROM high_scores
            WHERE maze_size = ?
            ORDER BY solve_time_ms ASC
            LIMIT ?
        """;

        List<Map<String, Object>> scores = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, mazeSize);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> score = new HashMap<>();
                score.put("playerName", rs.getString("player_name"));
                score.put("mazeSize", rs.getString("maze_size"));
                score.put("solveTimeMs", rs.getLong("solve_time_ms"));
                score.put("algorithm", rs.getString("algorithm"));
                score.put("createdAt", rs.getString("created_at"));
                scores.add(score);
            }
        } catch (SQLException e) {
            System.err.println("Failed to retrieve scores by size: " + e.getMessage());
        }

        return scores;
    }

    // ─── Player Attempts ───────────────────────────────────────────────────────

    public void addAttempt(String playerName, String mazeSize, long durationMs, int steps, boolean completed) {
        String insertSQL = """
            INSERT INTO player_attempts (player_name, maze_size, duration_ms, steps, completed, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, playerName);
            pstmt.setString(2, mazeSize);
            pstmt.setLong(3, durationMs);
            pstmt.setInt(4, steps);
            pstmt.setInt(5, completed ? 1 : 0);
            pstmt.setString(6, timestamp);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to add attempt: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getTopAttempts(int limit) {
        String querySQL = """
            SELECT player_name, maze_size, duration_ms, steps, created_at
            FROM player_attempts
            WHERE completed = 1
            ORDER BY steps ASC, duration_ms ASC
            LIMIT ?
        """;

        List<Map<String, Object>> attempts = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> attempt = new HashMap<>();
                attempt.put("playerName", rs.getString("player_name"));
                attempt.put("mazeSize", rs.getString("maze_size"));
                attempt.put("durationMs", rs.getLong("duration_ms"));
                attempt.put("steps", rs.getInt("steps"));
                attempt.put("createdAt", rs.getString("created_at"));
                attempts.add(attempt);
            }
        } catch (SQLException e) {
            System.err.println("Failed to retrieve attempts: " + e.getMessage());
        }

        return attempts;
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database: " + e.getMessage());
        }
    }
}
