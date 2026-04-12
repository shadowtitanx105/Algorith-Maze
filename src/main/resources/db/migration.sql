-- Maze Master — Relational Schema (Reference Only)
-- Auto-created by Database.java on first run.
-- Do NOT commit maze_master.db to version control.
--
-- Relational Schema (underline = PK, * = FK):
--   PLAYER     ( player_id, name, created )
--   MAZE       ( maze_id, rows, cols, grid_data, created_at )
--   SAVES      ( *player_id, *maze_id, label, saved_at )          ← associative entity (M:N)
--   ALGO_RUN   ( algo_id, *player_id, *maze_id, maze_size, algo_name, solve_time, path_length, created_at )
--   MANUAL     ( attempt_id, *player_id, *maze_id, maze_size, duration, steps, created_time )
--   PERSOBEST  ( *player_id, maze_size, steps, duration, last_update )  ← weak entity of PLAYER

PRAGMA foreign_keys = ON;

-- ── 1. PLAYER  (strong entity) ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS player (
    player_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL UNIQUE,
    created    TEXT    NOT NULL
);

-- ── 2. MAZE  (strong entity — independent of any single player) ───────────
CREATE TABLE IF NOT EXISTS maze (
    maze_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    rows       INTEGER NOT NULL,
    cols       INTEGER NOT NULL,
    grid_data  TEXT    NOT NULL,
    created_at TEXT    NOT NULL
);

-- ── 3. SAVES  (associative entity resolving M:N between PLAYER and MAZE) ──
--    Composite PK (player_id, maze_id)
--    Relationship-owned attributes: label, saved_at
CREATE TABLE IF NOT EXISTS saves (
    player_id  INTEGER NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
    maze_id    INTEGER NOT NULL REFERENCES maze(maze_id)     ON DELETE CASCADE,
    label      TEXT    NOT NULL,
    saved_at   TEXT    NOT NULL,
    PRIMARY KEY (player_id, maze_id)
);

CREATE INDEX IF NOT EXISTS idx_saves_maze ON saves(maze_id);

-- ── 4. ALGO_RUN  (strong entity) ──────────────────────────────────────────
--    maze_id nullable: a run may exist for an unsaved (in-memory) maze
CREATE TABLE IF NOT EXISTS algo_run (
    algo_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id   INTEGER NOT NULL REFERENCES player(player_id),
    maze_id     INTEGER          REFERENCES maze(maze_id) ON DELETE SET NULL,
    maze_size   TEXT    NOT NULL,
    algo_name   TEXT    NOT NULL,
    solve_time  INTEGER NOT NULL,
    path_length INTEGER NOT NULL,
    created_at  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_algo_time ON algo_run(solve_time ASC);

-- ── 5. MANUAL  (strong entity) ────────────────────────────────────────────
--    maze_id nullable: a play may exist for an unsaved (in-memory) maze
CREATE TABLE IF NOT EXISTS manual (
    attempt_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id    INTEGER NOT NULL REFERENCES player(player_id),
    maze_id      INTEGER          REFERENCES maze(maze_id) ON DELETE SET NULL,
    maze_size    TEXT    NOT NULL,
    duration     INTEGER NOT NULL,
    steps        INTEGER NOT NULL,
    created_time TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_manual_steps ON manual(steps ASC);

-- ── 6. PERSOBEST  (weak entity of PLAYER) ─────────────────────────────────
--    Composite PK: (player_id, maze_size) — maze_size is the partial key
--    Upserted automatically on every MANUAL insert
CREATE TABLE IF NOT EXISTS persobest (
    player_id   INTEGER NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
    maze_size   TEXT    NOT NULL,
    steps       INTEGER NOT NULL,
    duration    INTEGER NOT NULL,
    last_update TEXT    NOT NULL,
    PRIMARY KEY (player_id, maze_size)
);
