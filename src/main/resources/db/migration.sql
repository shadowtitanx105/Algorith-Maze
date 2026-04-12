-- Maze Master Normalised Database Schema
-- Auto-created by Database.java on first run.
-- This file is for reference only — do NOT commit maze_master.db to Git.

-- ════════════════════════════════════════════════════════════════════════════
-- ENTITY 1: PLAYER  (strong entity)
-- ════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS player (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL UNIQUE,
    created_at TEXT    NOT NULL
);

-- ════════════════════════════════════════════════════════════════════════════
-- ENTITY 2: MAZE  (WEAK entity of PLAYER — existence depends on its owner)
--   Partial key: label
--   ON DELETE CASCADE: deleting a player removes all their saved mazes.
-- ════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS maze (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id  INTEGER NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    rows       INTEGER NOT NULL,
    cols       INTEGER NOT NULL,
    grid_data  TEXT    NOT NULL,   -- full maze JSON serialised as a string
    label      TEXT    NOT NULL,
    created_at TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_maze_player ON maze(player_id);

-- ════════════════════════════════════════════════════════════════════════════
-- ENTITY 3: ALGORITHM_RUN  (strong entity)
--   FK to player (mandatory) and maze (optional — NULL if maze not saved).
-- ════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS algorithm_run (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id     INTEGER NOT NULL REFERENCES player(id),
    maze_id       INTEGER          REFERENCES maze(id) ON DELETE SET NULL,
    maze_size     TEXT    NOT NULL,   -- denormalised for display when maze_id is NULL
    algorithm     TEXT    NOT NULL,
    solve_time_ms INTEGER NOT NULL,
    path_length   INTEGER NOT NULL,
    created_at    TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_algo_time ON algorithm_run(solve_time_ms ASC);

-- ════════════════════════════════════════════════════════════════════════════
-- ENTITY 4: MANUAL_ATTEMPT  (strong entity)
--   FK to player (mandatory) and maze (optional).
-- ════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS manual_attempt (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id   INTEGER NOT NULL REFERENCES player(id),
    maze_id     INTEGER          REFERENCES maze(id) ON DELETE SET NULL,
    maze_size   TEXT    NOT NULL,   -- denormalised for display when maze_id is NULL
    duration_ms INTEGER NOT NULL,
    steps       INTEGER NOT NULL,
    created_at  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_manual_steps ON manual_attempt(steps ASC);

-- ════════════════════════════════════════════════════════════════════════════
-- ENTITY 5: PERSONAL_BEST  (WEAK entity of PLAYER)
--   Composite PK: (player_id, maze_size) — maze_size is the partial key.
--   ON DELETE CASCADE: deleting a player removes their personal bests.
--   Upserted automatically whenever a manual_attempt is recorded.
-- ════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS personal_best (
    player_id        INTEGER NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    maze_size        TEXT    NOT NULL,
    best_steps       INTEGER NOT NULL,
    best_duration_ms INTEGER NOT NULL,
    last_updated     TEXT    NOT NULL,
    PRIMARY KEY (player_id, maze_size)
);

-- ════════════════════════════════════════════════════════════════════════════
-- RELATIONAL SCHEMA SUMMARY
-- ════════════════════════════════════════════════════════════════════════════
--
-- PLAYER         (id, name, created_at)
-- MAZE           (id, *player_id, rows, cols, grid_data, label, created_at)
-- ALGORITHM_RUN  (id, *player_id, *maze_id, maze_size, algorithm, solve_time_ms, path_length, created_at)
-- MANUAL_ATTEMPT (id, *player_id, *maze_id, maze_size, duration_ms, steps, created_at)
-- PERSONAL_BEST  (*player_id, maze_size, best_steps, best_duration_ms, last_updated)
--
-- Underline = PK   * = FK
-- MAZE and PERSONAL_BEST are weak entities (identifying relationships with PLAYER).
