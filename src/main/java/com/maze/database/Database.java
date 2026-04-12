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
            initializeDatabase(); // tables must exist before migration
            migrate();            // port any previous-schema data
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
    // SCHEMA — 6 tables matching the relational schema
    // ════════════════════════════════════════════════════════════════════════

    private void initializeDatabase() {
        String[] tables = {

            // 1. PLAYER  (strong entity)
            """
            CREATE TABLE IF NOT EXISTS player (
                player_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                name       TEXT    NOT NULL UNIQUE,
                created    TEXT    NOT NULL
            )
            """,

            // 2. MAZE  (strong entity — independent of any single player)
            """
            CREATE TABLE IF NOT EXISTS maze (
                maze_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                rows       INTEGER NOT NULL,
                cols       INTEGER NOT NULL,
                grid_data  TEXT    NOT NULL,
                created_at TEXT    NOT NULL
            )
            """,

            // 3. SAVES  (associative entity resolving M:N between PLAYER and MAZE)
            //    Composite PK: (player_id, maze_id)
            //    Relationship attributes: label, saved_at
            """
            CREATE TABLE IF NOT EXISTS saves (
                player_id  INTEGER NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
                maze_id    INTEGER NOT NULL REFERENCES maze(maze_id)     ON DELETE CASCADE,
                label      TEXT    NOT NULL,
                saved_at   TEXT    NOT NULL,
                PRIMARY KEY (player_id, maze_id)
            )
            """,

            // 4. ALGO_RUN  (strong entity)
            //    maze_id is nullable — a run can occur on an unsaved maze
            """
            CREATE TABLE IF NOT EXISTS algo_run (
                algo_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id   INTEGER NOT NULL REFERENCES player(player_id),
                maze_id     INTEGER          REFERENCES maze(maze_id) ON DELETE SET NULL,
                maze_size   TEXT    NOT NULL,
                algo_name   TEXT    NOT NULL,
                solve_time  INTEGER NOT NULL,
                path_length INTEGER NOT NULL,
                created_at  TEXT    NOT NULL
            )
            """,

            // 5. MANUAL  (strong entity)
            //    maze_id is nullable — a play can occur on an unsaved maze
            """
            CREATE TABLE IF NOT EXISTS manual (
                attempt_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id    INTEGER NOT NULL REFERENCES player(player_id),
                maze_id      INTEGER          REFERENCES maze(maze_id) ON DELETE SET NULL,
                maze_size    TEXT    NOT NULL,
                duration     INTEGER NOT NULL,
                steps        INTEGER NOT NULL,
                created_time TEXT    NOT NULL
            )
            """,

            // 6. PERSOBEST  (weak entity of PLAYER)
            //    Composite PK: (player_id, maze_size) — maze_size is the partial key
            """
            CREATE TABLE IF NOT EXISTS persobest (
                player_id   INTEGER NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
                maze_size   TEXT    NOT NULL,
                steps       INTEGER NOT NULL,
                duration    INTEGER NOT NULL,
                last_update TEXT    NOT NULL,
                PRIMARY KEY (player_id, maze_size)
            )
            """
        };

        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_algo_time    ON algo_run(solve_time ASC)",
            "CREATE INDEX IF NOT EXISTS idx_manual_steps ON manual(steps ASC)",
            "CREATE INDEX IF NOT EXISTS idx_saves_maze   ON saves(maze_id)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : tables)  stmt.execute(sql);
            for (String sql : indexes) stmt.execute(sql);
            System.out.println("✅ 6 database tables initialised.");
        } catch (SQLException e) {
            System.err.println("Schema creation failed: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MIGRATION — from previous schema versions
    // ════════════════════════════════════════════════════════════════════════

    private void migrate() {
        try {
            boolean hasAlgoRun      = tableExists("algorithm_run");
            boolean hasManualAttempt = tableExists("manual_attempt");
            boolean hasPersBest     = tableExists("personal_best");
            boolean mazeHasPlayerCol = tableExists("maze") && columnExists("maze", "player_id");

            if (!hasAlgoRun && !hasManualAttempt && !hasPersBest && !mazeHasPlayerCol) return;

            System.out.println("📦 Previous schema detected — migrating to relational schema...");

            if (hasAlgoRun) {
                String q = "SELECT player_id, maze_id, maze_size, algorithm, solve_time_ms, path_length, created_at FROM algorithm_run";
                String i = "INSERT INTO algo_run (player_id, maze_id, maze_size, algo_name, solve_time, path_length, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(q)) {
                    while (rs.next()) {
                        try (PreparedStatement p = connection.prepareStatement(i)) {
                            p.setInt(1, rs.getInt("player_id"));
                            int mid = rs.getInt("maze_id"); if (rs.wasNull()) p.setNull(2, Types.INTEGER); else p.setInt(2, mid);
                            p.setString(3, rs.getString("maze_size"));
                            p.setString(4, rs.getString("algorithm"));
                            p.setLong(5, rs.getLong("solve_time_ms"));
                            p.setInt(6, rs.getInt("path_length"));
                            p.setString(7, rs.getString("created_at"));
                            p.executeUpdate();
                        }
                    }
                }
                try (Statement s = connection.createStatement()) { s.execute("DROP TABLE algorithm_run"); }
                System.out.println("   ↳ algorithm_run → algo_run");
            }

            if (hasManualAttempt) {
                String q = "SELECT player_id, maze_id, maze_size, duration_ms, steps, created_at FROM manual_attempt";
                String i = "INSERT INTO manual (player_id, maze_id, maze_size, duration, steps, created_time) VALUES (?, ?, ?, ?, ?, ?)";
                try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(q)) {
                    while (rs.next()) {
                        try (PreparedStatement p = connection.prepareStatement(i)) {
                            p.setInt(1, rs.getInt("player_id"));
                            int mid = rs.getInt("maze_id"); if (rs.wasNull()) p.setNull(2, Types.INTEGER); else p.setInt(2, mid);
                            p.setString(3, rs.getString("maze_size"));
                            p.setLong(4, rs.getLong("duration_ms"));
                            p.setInt(5, rs.getInt("steps"));
                            p.setString(6, rs.getString("created_at"));
                            p.executeUpdate();
                        }
                    }
                }
                try (Statement s = connection.createStatement()) { s.execute("DROP TABLE manual_attempt"); }
                System.out.println("   ↳ manual_attempt → manual");
            }

            if (hasPersBest) {
                String q = "SELECT player_id, maze_size, best_steps, best_duration_ms, last_updated FROM personal_best";
                String i = "INSERT INTO persobest (player_id, maze_size, steps, duration, last_update) VALUES (?, ?, ?, ?, ?)";
                try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(q)) {
                    while (rs.next()) {
                        try (PreparedStatement p = connection.prepareStatement(i)) {
                            p.setInt(1, rs.getInt("player_id")); p.setString(2, rs.getString("maze_size"));
                            p.setInt(3, rs.getInt("best_steps")); p.setLong(4, rs.getLong("best_duration_ms"));
                            p.setString(5, rs.getString("last_updated"));
                            p.executeUpdate();
                        }
                    }
                }
                try (Statement s = connection.createStatement()) { s.execute("DROP TABLE personal_best"); }
                System.out.println("   ↳ personal_best → persobest");
            }

            if (mazeHasPlayerCol) {
                // Move player_id + label from maze into saves, then rebuild maze without those columns
                String q = "SELECT maze_id, player_id, label, created_at FROM maze WHERE player_id IS NOT NULL";
                String i = "INSERT OR IGNORE INTO saves (player_id, maze_id, label, saved_at) VALUES (?, ?, ?, ?)";
                try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(q)) {
                    while (rs.next()) {
                        try (PreparedStatement p = connection.prepareStatement(i)) {
                            p.setInt(1, rs.getInt("player_id")); p.setInt(2, rs.getInt("maze_id"));
                            p.setString(3, rs.getString("label")); p.setString(4, rs.getString("created_at"));
                            p.executeUpdate();
                        }
                    }
                }
                try (Statement s = connection.createStatement()) {
                    s.execute("CREATE TABLE maze_new (maze_id INTEGER PRIMARY KEY AUTOINCREMENT, rows INTEGER NOT NULL, cols INTEGER NOT NULL, grid_data TEXT NOT NULL, created_at TEXT NOT NULL)");
                    s.execute("INSERT INTO maze_new SELECT maze_id, rows, cols, grid_data, created_at FROM maze");
                    s.execute("DROP TABLE maze");
                    s.execute("ALTER TABLE maze_new RENAME TO maze");
                }
                System.out.println("   ↳ maze restructured — player_id/label moved to saves");
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

    private boolean columnExists(String table, String column) throws SQLException {
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equals(rs.getString("name"))) return true;
            }
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PLAYER
    // ════════════════════════════════════════════════════════════════════════

    public int getOrCreatePlayer(String name) throws SQLException {
        try (PreparedStatement ins = connection.prepareStatement(
                "INSERT OR IGNORE INTO player (name, created) VALUES (?, ?)")) {
            ins.setString(1, name);
            ins.setString(2, now());
            ins.executeUpdate();
        }
        try (PreparedStatement sel = connection.prepareStatement(
                "SELECT player_id FROM player WHERE name = ?")) {
            sel.setString(1, name);
            ResultSet rs = sel.executeQuery();
            if (rs.next()) return rs.getInt("player_id");
        }
        throw new SQLException("Could not resolve player: " + name);
    }

    // ════════════════════════════════════════════════════════════════════════
    // MAZE  (strong entity)
    // ════════════════════════════════════════════════════════════════════════

    public int insertMaze(int rows, int cols, String gridJson) {
        String sql = "INSERT INTO maze (rows, cols, grid_data, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement p = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, rows);
            p.setInt(2, cols);
            p.setString(3, gridJson);
            p.setString(4, now());
            p.executeUpdate();
            ResultSet keys = p.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("Failed to insert maze: " + e.getMessage());
        }
        return -1;
    }

    public String getMazeById(int mazeId) {
        try (PreparedStatement p = connection.prepareStatement(
                "SELECT grid_data FROM maze WHERE maze_id = ?")) {
            p.setInt(1, mazeId);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getString("grid_data");
        } catch (SQLException e) {
            System.err.println("Failed to get maze: " + e.getMessage());
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    // SAVES  (associative entity — M:N between PLAYER and MAZE)
    // ════════════════════════════════════════════════════════════════════════

    public void addSave(int playerId, int mazeId, String label) {
        String sql = "INSERT OR REPLACE INTO saves (player_id, maze_id, label, saved_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, playerId);
            p.setInt(2, mazeId);
            p.setString(3, label);
            p.setString(4, now());
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to add save: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getSavedMazes() {
        String sql = """
            SELECT m.maze_id, s.label, m.rows, m.cols, s.saved_at, p.name AS player_name
            FROM saves s
            JOIN maze   m ON s.maze_id   = m.maze_id
            JOIN player p ON s.player_id = p.player_id
            ORDER BY s.saved_at DESC
        """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("mazeId",     rs.getInt("maze_id"));
                row.put("label",      rs.getString("label"));
                row.put("rows",       rs.getInt("rows"));
                row.put("cols",       rs.getInt("cols"));
                row.put("playerName", rs.getString("player_name"));
                row.put("savedAt",    rs.getString("saved_at"));
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get saved mazes: " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ALGO_RUN
    // ════════════════════════════════════════════════════════════════════════

    public void addAlgoRun(int playerId, Integer mazeId, String mazeSize,
                           String algoName, long solveTime, int pathLength) {
        String sql = "INSERT INTO algo_run (player_id, maze_id, maze_size, algo_name, solve_time, path_length, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, playerId);
            if (mazeId != null) p.setInt(2, mazeId); else p.setNull(2, Types.INTEGER);
            p.setString(3, mazeSize);
            p.setString(4, algoName);
            p.setLong(5, solveTime);
            p.setInt(6, pathLength);
            p.setString(7, now());
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to add algo run: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getTopAlgoRuns(int limit) {
        String sql = """
            SELECT ar.solve_time, ar.maze_size, ar.algo_name, ar.path_length,
                   p.name AS player_name
            FROM algo_run ar
            JOIN player p ON ar.player_id = p.player_id
            ORDER BY ar.solve_time ASC
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
                row.put("algoName",   rs.getString("algo_name"));
                row.put("solveTime",  rs.getLong("solve_time"));
                row.put("pathLength", rs.getInt("path_length"));
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get top algo runs: " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MANUAL
    // ════════════════════════════════════════════════════════════════════════

    public void addManual(int playerId, Integer mazeId, String mazeSize, long duration, int steps) {
        String sql = "INSERT INTO manual (player_id, maze_id, maze_size, duration, steps, created_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, playerId);
            if (mazeId != null) p.setInt(2, mazeId); else p.setNull(2, Types.INTEGER);
            p.setString(3, mazeSize);
            p.setLong(4, duration);
            p.setInt(5, steps);
            p.setString(6, now());
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to add manual attempt: " + e.getMessage());
        }
        upsertPersoBest(playerId, mazeSize, steps, duration);
    }

    public List<Map<String, Object>> getTopManual(int limit) {
        String sql = """
            SELECT m.steps, m.duration, m.maze_size, p.name AS player_name
            FROM manual m
            JOIN player p ON m.player_id = p.player_id
            ORDER BY m.steps ASC, m.duration ASC
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
                row.put("duration",   rs.getLong("duration"));
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get top manual: " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PERSOBEST  (weak entity of PLAYER — composite PK: player_id + maze_size)
    // ════════════════════════════════════════════════════════════════════════

    public void upsertPersoBest(int playerId, String mazeSize, int steps, long duration) {
        String sel = "SELECT steps, duration FROM persobest WHERE player_id = ? AND maze_size = ?";
        String ins = "INSERT INTO persobest (player_id, maze_size, steps, duration, last_update) VALUES (?, ?, ?, ?, ?)";
        String upd = "UPDATE persobest SET steps = ?, duration = ?, last_update = ? WHERE player_id = ? AND maze_size = ?";
        String ts  = now();
        try {
            try (PreparedStatement s = connection.prepareStatement(sel)) {
                s.setInt(1, playerId); s.setString(2, mazeSize);
                ResultSet rs = s.executeQuery();
                if (!rs.next()) {
                    try (PreparedStatement p = connection.prepareStatement(ins)) {
                        p.setInt(1, playerId); p.setString(2, mazeSize);
                        p.setInt(3, steps); p.setLong(4, duration); p.setString(5, ts);
                        p.executeUpdate();
                    }
                } else {
                    int  exSteps = rs.getInt("steps");
                    long exDur   = rs.getLong("duration");
                    if (steps < exSteps || (steps == exSteps && duration < exDur)) {
                        try (PreparedStatement p = connection.prepareStatement(upd)) {
                            p.setInt(1, steps); p.setLong(2, duration); p.setString(3, ts);
                            p.setInt(4, playerId); p.setString(5, mazeSize);
                            p.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to upsert persobest: " + e.getMessage());
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
