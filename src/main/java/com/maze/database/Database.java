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
            enableForeignKeys();
            initializeDatabase(); // tables must exist BEFORE migration inserts into them
            migrate();            // then port any legacy data into the new schema
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private void enableForeignKeys() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SCHEMA CREATION
    // ════════════════════════════════════════════════════════════════════════

    private void initializeDatabase() {
        String[] tables = {
            // 1. PLAYER — strong entity
            """
            CREATE TABLE IF NOT EXISTS player (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                name       TEXT    NOT NULL UNIQUE,
                created_at TEXT    NOT NULL
            )
            """,
            // 2. MAZE — weak entity of PLAYER (cascades on player delete)
            """
            CREATE TABLE IF NOT EXISTS maze (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id  INTEGER NOT NULL REFERENCES player(id) ON DELETE CASCADE,
                rows       INTEGER NOT NULL,
                cols       INTEGER NOT NULL,
                grid_data  TEXT    NOT NULL,
                label      TEXT    NOT NULL,
                created_at TEXT    NOT NULL
            )
            """,
            // 3. ALGORITHM_RUN — strong entity; FK to player + nullable FK to maze
            """
            CREATE TABLE IF NOT EXISTS algorithm_run (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id     INTEGER NOT NULL REFERENCES player(id),
                maze_id       INTEGER          REFERENCES maze(id) ON DELETE SET NULL,
                maze_size     TEXT    NOT NULL,
                algorithm     TEXT    NOT NULL,
                solve_time_ms INTEGER NOT NULL,
                path_length   INTEGER NOT NULL,
                created_at    TEXT    NOT NULL
            )
            """,
            // 4. MANUAL_ATTEMPT — strong entity; FK to player + nullable FK to maze
            """
            CREATE TABLE IF NOT EXISTS manual_attempt (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id   INTEGER NOT NULL REFERENCES player(id),
                maze_id     INTEGER          REFERENCES maze(id) ON DELETE SET NULL,
                maze_size   TEXT    NOT NULL,
                duration_ms INTEGER NOT NULL,
                steps       INTEGER NOT NULL,
                created_at  TEXT    NOT NULL
            )
            """,
            // 5. PERSONAL_BEST — weak entity of PLAYER; composite PK (player_id, maze_size)
            """
            CREATE TABLE IF NOT EXISTS personal_best (
                player_id        INTEGER NOT NULL REFERENCES player(id) ON DELETE CASCADE,
                maze_size        TEXT    NOT NULL,
                best_steps       INTEGER NOT NULL,
                best_duration_ms INTEGER NOT NULL,
                last_updated     TEXT    NOT NULL,
                PRIMARY KEY (player_id, maze_size)
            )
            """
        };

        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_algo_time   ON algorithm_run(solve_time_ms ASC)",
            "CREATE INDEX IF NOT EXISTS idx_manual_steps ON manual_attempt(steps ASC)",
            "CREATE INDEX IF NOT EXISTS idx_maze_player  ON maze(player_id)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : tables)  stmt.execute(sql);
            for (String sql : indexes) stmt.execute(sql);
            System.out.println("✅ 5 database tables initialised.");
        } catch (SQLException e) {
            System.err.println("Schema creation failed: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MIGRATION — from legacy flat tables
    // ════════════════════════════════════════════════════════════════════════

    private void migrate() {
        try {
            boolean hasOldScores   = tableExists("high_scores");
            boolean hasOldAttempts = tableExists("player_attempts");
            if (!hasOldScores && !hasOldAttempts) return;

            System.out.println("📦 Legacy data detected — migrating to normalised schema...");

            if (hasOldScores) {
                String query = "SELECT player_name, maze_size, solve_time_ms, algorithm, created_at FROM high_scores";
                try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(query)) {
                    while (rs.next()) {
                        int pid = getOrCreatePlayer(rs.getString("player_name"));
                        String sql = "INSERT INTO algorithm_run (player_id, maze_id, maze_size, algorithm, solve_time_ms, path_length, created_at) VALUES (?, NULL, ?, ?, ?, 0, ?)";
                        try (PreparedStatement p = connection.prepareStatement(sql)) {
                            p.setInt(1, pid);
                            p.setString(2, rs.getString("maze_size"));
                            p.setString(3, rs.getString("algorithm"));
                            p.setLong(4, rs.getLong("solve_time_ms"));
                            p.setString(5, rs.getString("created_at"));
                            p.executeUpdate();
                        }
                    }
                }
                try (Statement s = connection.createStatement()) { s.execute("DROP TABLE high_scores"); }
                System.out.println("   ↳ high_scores migrated and dropped.");
            }

            if (hasOldAttempts) {
                String query = "SELECT player_name, maze_size, duration_ms, steps, created_at FROM player_attempts WHERE completed = 1";
                try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(query)) {
                    while (rs.next()) {
                        int pid = getOrCreatePlayer(rs.getString("player_name"));
                        String sql = "INSERT INTO manual_attempt (player_id, maze_id, maze_size, duration_ms, steps, created_at) VALUES (?, NULL, ?, ?, ?, ?)";
                        try (PreparedStatement p = connection.prepareStatement(sql)) {
                            p.setInt(1, pid);
                            p.setString(2, rs.getString("maze_size"));
                            p.setLong(3, rs.getLong("duration_ms"));
                            p.setInt(4, rs.getInt("steps"));
                            p.setString(5, rs.getString("created_at"));
                            p.executeUpdate();
                        }
                    }
                }
                try (Statement s = connection.createStatement()) { s.execute("DROP TABLE player_attempts"); }
                System.out.println("   ↳ player_attempts migrated and dropped.");
            }
            System.out.println("✅ Migration complete.");
        } catch (SQLException e) {
            System.err.println("Migration error: " + e.getMessage());
        }
    }

    private boolean tableExists(String name) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            p.setString(1, name);
            return p.executeQuery().next();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PLAYER
    // ════════════════════════════════════════════════════════════════════════

    public int getOrCreatePlayer(String name) throws SQLException {
        try (PreparedStatement ins = connection.prepareStatement(
                "INSERT OR IGNORE INTO player (name, created_at) VALUES (?, ?)")) {
            ins.setString(1, name);
            ins.setString(2, now());
            ins.executeUpdate();
        }
        try (PreparedStatement sel = connection.prepareStatement(
                "SELECT id FROM player WHERE name = ?")) {
            sel.setString(1, name);
            ResultSet rs = sel.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }
        throw new SQLException("Could not resolve player: " + name);
    }

    // ════════════════════════════════════════════════════════════════════════
    // MAZE  (weak entity of PLAYER)
    // ════════════════════════════════════════════════════════════════════════

    public int saveMaze(int playerId, int rows, int cols, String gridJson, String label) {
        String sql = "INSERT INTO maze (player_id, rows, cols, grid_data, label, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, playerId);
            p.setInt(2, rows);
            p.setInt(3, cols);
            p.setString(4, gridJson);
            p.setString(5, label);
            p.setString(6, now());
            p.executeUpdate();
            ResultSet keys = p.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("Failed to save maze: " + e.getMessage());
        }
        return -1;
    }

    public String getMazeById(int id) {
        try (PreparedStatement p = connection.prepareStatement(
                "SELECT grid_data FROM maze WHERE id = ?")) {
            p.setInt(1, id);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getString("grid_data");
        } catch (SQLException e) {
            System.err.println("Failed to get maze: " + e.getMessage());
        }
        return null;
    }

    public List<Map<String, Object>> getSavedMazes() {
        String sql = """
            SELECT m.id, m.label, m.rows, m.cols, m.created_at, p.name AS player_name
            FROM maze m
            JOIN player p ON m.player_id = p.id
            ORDER BY m.created_at DESC
        """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",         rs.getInt("id"));
                row.put("label",      rs.getString("label"));
                row.put("rows",       rs.getInt("rows"));
                row.put("cols",       rs.getInt("cols"));
                row.put("playerName", rs.getString("player_name"));
                row.put("createdAt",  rs.getString("created_at"));
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get saved mazes: " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ALGORITHM_RUN
    // ════════════════════════════════════════════════════════════════════════

    public void addAlgorithmRun(int playerId, Integer mazeId, String mazeSize,
                                String algorithm, long solveTimeMs, int pathLength) {
        String sql = "INSERT INTO algorithm_run (player_id, maze_id, maze_size, algorithm, solve_time_ms, path_length, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, playerId);
            if (mazeId != null) p.setInt(2, mazeId); else p.setNull(2, Types.INTEGER);
            p.setString(3, mazeSize);
            p.setString(4, algorithm);
            p.setLong(5, solveTimeMs);
            p.setInt(6, pathLength);
            p.setString(7, now());
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to add algorithm run: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getTopAlgorithmRuns(int limit) {
        String sql = """
            SELECT ar.solve_time_ms, ar.maze_size, ar.algorithm, ar.path_length,
                   p.name AS player_name
            FROM algorithm_run ar
            JOIN player p ON ar.player_id = p.id
            ORDER BY ar.solve_time_ms ASC
            LIMIT ?
        """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, limit);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("playerName",  rs.getString("player_name"));
                row.put("mazeSize",    rs.getString("maze_size"));
                row.put("algorithm",   rs.getString("algorithm"));
                row.put("solveTimeMs", rs.getLong("solve_time_ms"));
                row.put("pathLength",  rs.getInt("path_length"));
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get top algorithm runs: " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MANUAL_ATTEMPT
    // ════════════════════════════════════════════════════════════════════════

    public void addManualAttempt(int playerId, Integer mazeId, String mazeSize,
                                 long durationMs, int steps) {
        String sql = "INSERT INTO manual_attempt (player_id, maze_id, maze_size, duration_ms, steps, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, playerId);
            if (mazeId != null) p.setInt(2, mazeId); else p.setNull(2, Types.INTEGER);
            p.setString(3, mazeSize);
            p.setLong(4, durationMs);
            p.setInt(5, steps);
            p.setString(6, now());
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to add manual attempt: " + e.getMessage());
        }
        upsertPersonalBest(playerId, mazeSize, steps, durationMs);
    }

    public List<Map<String, Object>> getTopManualAttempts(int limit) {
        String sql = """
            SELECT ma.steps, ma.duration_ms, ma.maze_size, p.name AS player_name
            FROM manual_attempt ma
            JOIN player p ON ma.player_id = p.id
            ORDER BY ma.steps ASC, ma.duration_ms ASC
            LIMIT ?
        """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, limit);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("playerName", rs.getString("player_name"));
                row.put("mazeSize",   rs.getString("maze_size"));
                row.put("steps",      rs.getInt("steps"));
                row.put("durationMs", rs.getLong("duration_ms"));
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get top manual attempts: " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PERSONAL_BEST  (weak entity of PLAYER — composite PK: player_id + maze_size)
    // ════════════════════════════════════════════════════════════════════════

    public void upsertPersonalBest(int playerId, String mazeSize, int steps, long durationMs) {
        String selectSQL = "SELECT best_steps, best_duration_ms FROM personal_best WHERE player_id = ? AND maze_size = ?";
        String insertSQL = "INSERT INTO personal_best (player_id, maze_size, best_steps, best_duration_ms, last_updated) VALUES (?, ?, ?, ?, ?)";
        String updateSQL = "UPDATE personal_best SET best_steps = ?, best_duration_ms = ?, last_updated = ? WHERE player_id = ? AND maze_size = ?";
        String ts = now();

        try {
            try (PreparedStatement sel = connection.prepareStatement(selectSQL)) {
                sel.setInt(1, playerId);
                sel.setString(2, mazeSize);
                ResultSet rs = sel.executeQuery();

                if (!rs.next()) {
                    // First record for this player + size combination
                    try (PreparedStatement ins = connection.prepareStatement(insertSQL)) {
                        ins.setInt(1, playerId);
                        ins.setString(2, mazeSize);
                        ins.setInt(3, steps);
                        ins.setLong(4, durationMs);
                        ins.setString(5, ts);
                        ins.executeUpdate();
                    }
                } else {
                    int  existingSteps = rs.getInt("best_steps");
                    long existingMs    = rs.getLong("best_duration_ms");
                    boolean isNewBest  = steps < existingSteps
                            || (steps == existingSteps && durationMs < existingMs);

                    if (isNewBest) {
                        try (PreparedStatement upd = connection.prepareStatement(updateSQL)) {
                            upd.setInt(1, steps);
                            upd.setLong(2, durationMs);
                            upd.setString(3, ts);
                            upd.setInt(4, playerId);
                            upd.setString(5, mazeSize);
                            upd.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to upsert personal best: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ════════════════════════════════════════════════════════════════════════

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            System.err.println("Failed to close database: " + e.getMessage());
        }
    }
}
