-- Maze Master Database Schema
-- This schema is auto-created by the Database class on first run.
-- Keep this file for reference only — do not track the .db file in Git.

-- ── Algorithm High Scores ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS high_scores (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    player_name    TEXT    NOT NULL,
    maze_size      TEXT    NOT NULL,
    solve_time_ms  INTEGER NOT NULL,
    algorithm      TEXT    NOT NULL,
    created_at     TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_solve_time ON high_scores(solve_time_ms);
CREATE INDEX IF NOT EXISTS idx_maze_size  ON high_scores(maze_size);
CREATE INDEX IF NOT EXISTS idx_created_at ON high_scores(created_at);

-- ── Manual Player Attempts ────────────────────────────────────────────────────
-- Only completed=1 rows are returned in leaderboards; abandoned runs are never written.
CREATE TABLE IF NOT EXISTS player_attempts (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    player_name TEXT    NOT NULL,
    maze_size   TEXT    NOT NULL,
    duration_ms INTEGER NOT NULL,
    steps       INTEGER NOT NULL,
    completed   INTEGER NOT NULL,   -- always 1 (wins only)
    created_at  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_attempts_steps ON player_attempts(steps ASC);
