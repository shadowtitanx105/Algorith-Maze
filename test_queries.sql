-- ════════════════════════════════════════════════════════════════════════════
-- Algorith-Maze  —  SQLite Test Script
-- Run with:  sqlite3 maze_master.db < test_queries.sql
--        or: sqlite3 maze_master.db   (then .read test_queries.sql)
-- ════════════════════════════════════════════════════════════════════════════

PRAGMA foreign_keys = ON;
.mode column
.headers on
.separator "  |  "


-- ════════════════════════════════════════════════════════════════════════════
-- SECTION 1 — DATA INJECTION
-- ════════════════════════════════════════════════════════════════════════════

-- ── 1.1 Players ─────────────────────────────────────────────────────────────
INSERT OR IGNORE INTO player (name, created) VALUES
    ('Alice',   '2026-04-01T10:00:00'),
    ('Bob',     '2026-04-02T11:30:00'),
    ('Charlie', '2026-04-03T09:15:00'),
    ('Diana',   '2026-04-04T14:00:00'),
    ('Eve',     '2026-04-05T08:45:00');

-- ── 1.2 Mazes (grid_data is a JSON string, abbreviated for test purposes) ───
INSERT OR IGNORE INTO maze (rows, cols, grid_data, created_at) VALUES
    (5,  5,  '{"rows":5,"cols":5,"cells":[]}',  '2026-04-01T10:05:00'),
    (10, 10, '{"rows":10,"cols":10,"cells":[]}', '2026-04-02T11:35:00'),
    (15, 15, '{"rows":15,"cols":15,"cells":[]}', '2026-04-03T09:20:00'),
    (20, 20, '{"rows":20,"cols":20,"cells":[]}', '2026-04-04T14:10:00'),
    (10, 10, '{"rows":10,"cols":10,"cells":[]}', '2026-04-05T08:50:00'),
    (5,  5,  '{"rows":5,"cols":5,"cells":[]}',   '2026-04-06T16:00:00');

-- ── 1.3 Saves (M:N — players saving mazes) ──────────────────────────────────
-- Alice saves mazes 1 and 2
INSERT OR IGNORE INTO saves (player_id, maze_id, label, saved_at) VALUES
    (1, 1, 'My First Maze',       '2026-04-01T10:10:00'),
    (1, 2, 'The Big One',         '2026-04-02T12:00:00');
-- Bob saves mazes 2 and 3
INSERT OR IGNORE INTO saves (player_id, maze_id, label, saved_at) VALUES
    (2, 2, 'Shared Maze',         '2026-04-02T13:00:00'),
    (2, 3, 'Huge Grid',           '2026-04-03T10:00:00');
-- Charlie saves maze 4
INSERT OR IGNORE INTO saves (player_id, maze_id, label, saved_at) VALUES
    (3, 4, 'Monster Maze',        '2026-04-04T15:00:00');
-- Diana saves mazes 1 and 5 (maze 1 is also Alice's — M:N in action)
INSERT OR IGNORE INTO saves (player_id, maze_id, label, saved_at) VALUES
    (4, 1, 'Borrowed Maze',       '2026-04-05T09:00:00'),
    (4, 5, 'Diana Quick',         '2026-04-05T09:30:00');

-- ── 1.4 Algo Runs ────────────────────────────────────────────────────────────
INSERT INTO algo_run (player_id, maze_id, maze_size, algo_name, solve_time, path_length, created_at) VALUES
-- Alice — multiple A* runs on small maze
    (1, 1, '5x5',   'astar',    12,  9,  '2026-04-01T10:20:00'),
    (1, 1, '5x5',   'astar',    10,  9,  '2026-04-01T10:25:00'),
    (1, 2, '10x10', 'astar',    85,  18, '2026-04-02T12:10:00'),
-- Bob — A* and Dijkstra
    (2, 2, '10x10', 'astar',    91,  18, '2026-04-02T13:10:00'),
    (2, 2, '10x10', 'dijkstra', 140, 18, '2026-04-02T13:20:00'),
    (2, 3, '15x15', 'astar',    220, 28, '2026-04-03T10:30:00'),
-- Charlie — big mazes
    (3, 3, '15x15', 'dijkstra', 310, 28, '2026-04-03T11:00:00'),
    (3, 4, '20x20', 'astar',    500, 38, '2026-04-04T15:20:00'),
    (3, 4, '20x20', 'dijkstra', 720, 38, '2026-04-04T15:40:00'),
-- Diana — fast solver
    (4, 1, '5x5',   'astar',    8,   9,  '2026-04-05T09:10:00'),
    (4, 5, '10x10', 'astar',    77,  17, '2026-04-05T09:40:00'),
-- Eve — runs on unsaved maze (maze_id = NULL)
    (5, NULL, '5x5',   'astar',    15, 9,  '2026-04-05T08:55:00'),
    (5, NULL, '10x10', 'dijkstra', 95, 19, '2026-04-05T09:05:00');

-- ── 1.5 Manual Attempts ──────────────────────────────────────────────────────
INSERT INTO manual (player_id, maze_id, maze_size, duration, steps, created_time) VALUES
-- Alice
    (1, 1, '5x5',   45000,  22, '2026-04-01T10:30:00'),
    (1, 2, '10x10', 120000, 45, '2026-04-02T12:30:00'),
-- Bob
    (2, 2, '10x10', 95000,  38, '2026-04-02T13:30:00'),
    (2, 3, '15x15', 240000, 65, '2026-04-03T11:00:00'),
-- Charlie — struggling on big maze
    (3, 4, '20x20', 600000, 110,'2026-04-04T16:00:00'),
    (3, 3, '15x15', 180000, 58, '2026-04-03T12:00:00'),
-- Diana — nimble player
    (4, 1, '5x5',   30000,  14, '2026-04-05T09:15:00'),
    (4, 5, '10x10', 88000,  33, '2026-04-05T09:50:00'),
-- Eve — a few tries on unsaved maze
    (5, NULL, '5x5',   55000, 25, '2026-04-05T09:00:00'),
    (5, NULL, '5x5',   48000, 21, '2026-04-05T09:20:00');

-- ── 1.6 Personal Bests (normally written by the app, seeding manually) ───────
INSERT OR REPLACE INTO persobest (player_id, maze_size, steps, duration, last_update) VALUES
    (1, '5x5',   22, 45000,  '2026-04-01T10:30:00'),
    (1, '10x10', 45, 120000, '2026-04-02T12:30:00'),
    (2, '10x10', 38, 95000,  '2026-04-02T13:30:00'),
    (2, '15x15', 65, 240000, '2026-04-03T11:00:00'),
    (3, '20x20', 110,600000, '2026-04-04T16:00:00'),
    (3, '15x15', 58, 180000, '2026-04-03T12:00:00'),
    (4, '5x5',   14, 30000,  '2026-04-05T09:15:00'),
    (4, '10x10', 33, 88000,  '2026-04-05T09:50:00'),
    (5, '5x5',   21, 48000,  '2026-04-05T09:20:00');


-- ════════════════════════════════════════════════════════════════════════════
-- SECTION 2 — COMPLEX QUERIES
-- ════════════════════════════════════════════════════════════════════════════

-- ── Q1: Global Algorithm Leaderboard (mirrors /api/scores/top) ───────────────
-- Shows how the index on solve_time carries over to a real JOIN
SELECT '=== Q1: Global Algorithm Leaderboard ===' AS "";

SELECT
    ROW_NUMBER() OVER (ORDER BY ar.solve_time ASC) AS rank,
    p.name          AS player,
    ar.maze_size,
    ar.algo_name,
    ar.solve_time   AS time_ms,
    ar.path_length  AS path_len
FROM algo_run ar
JOIN player p ON ar.player_id = p.player_id
ORDER BY ar.solve_time ASC
LIMIT 10;


-- ── Q2: Per-Algorithm Best Times (A* vs Dijkstra head-to-head) ───────────────
SELECT '=== Q2: Best Time per Algorithm per Maze Size ===' AS "";

SELECT
    maze_size,
    algo_name,
    MIN(solve_time) AS best_ms,
    AVG(solve_time) AS avg_ms,
    COUNT(*)        AS num_runs
FROM algo_run
GROUP BY maze_size, algo_name
ORDER BY maze_size, algo_name;


-- ── Q3: Manual Leaderboard for each Maze Size (partition ranking) ────────────
SELECT '=== Q3: Manual Leaderboard — Ranked within Each Maze Size ===' AS "";

SELECT
    maze_size,
    RANK() OVER (PARTITION BY maze_size ORDER BY steps ASC, duration ASC) AS size_rank,
    p.name AS player,
    m.steps,
    m.duration AS duration_ms
FROM manual m
JOIN player p ON m.player_id = p.player_id
ORDER BY maze_size, size_rank;


-- ── Q4: Players who saved a maze that someone else also saved (M:N check) ────
SELECT '=== Q4: Mazes Saved by Multiple Players (M:N shared saves) ===' AS "";

SELECT
    m.maze_id,
    m.rows || 'x' || m.cols        AS size,
    COUNT(DISTINCT s.player_id)    AS num_savers,
    GROUP_CONCAT(p.name, ', ')     AS saved_by
FROM saves s
JOIN maze   m ON s.maze_id   = m.maze_id
JOIN player p ON s.player_id = p.player_id
GROUP BY m.maze_id
HAVING COUNT(DISTINCT s.player_id) > 1
ORDER BY num_savers DESC;


-- ── Q5: Full Player Profile — joins all 4 player-related tables ──────────────
SELECT '=== Q5: Full Player Profile Summary ===' AS "";

SELECT
    p.name,
    COUNT(DISTINCT s.maze_id)           AS mazes_saved,
    COUNT(DISTINCT ar.algo_id)          AS algo_runs,
    MIN(ar.solve_time)                  AS best_algo_ms,
    COUNT(DISTINCT man.attempt_id)      AS manual_attempts,
    MIN(man.steps)                      AS best_manual_steps,
    (SELECT COUNT(*) FROM persobest pb WHERE pb.player_id = p.player_id)
                                        AS pb_records
FROM player p
LEFT JOIN saves   s   ON p.player_id = s.player_id
LEFT JOIN algo_run ar ON p.player_id = ar.player_id
LEFT JOIN manual  man ON p.player_id = man.player_id
GROUP BY p.player_id
ORDER BY best_algo_ms ASC NULLS LAST;

-- ── Q7: A* vs Dijkstra efficiency gap on same maze ───────────────────────────
SELECT '=== Q7: A* vs Dijkstra — Average Speed Ratio per Maze Size ===' AS "";

SELECT
    a.maze_size,
    ROUND(AVG(a.solve_time), 1)       AS avg_astar_ms,
    ROUND(AVG(d.solve_time), 1)       AS avg_dijkstra_ms,
    ROUND(AVG(d.solve_time) * 1.0
        / AVG(a.solve_time), 2)       AS dijkstra_slower_by
FROM algo_run a
JOIN algo_run d ON a.maze_size = d.maze_size
WHERE a.algo_name = 'astar'
  AND d.algo_name = 'dijkstra'
GROUP BY a.maze_size
ORDER BY a.maze_size;


-- ── Q8: Orphaned algo_runs with no corresponding save (nullable FK test) ──────
SELECT '=== Q8: Algo Runs on Unsaved Mazes (NULL maze_id — FK nullable test) ===' AS "";

SELECT
    p.name,
    ar.maze_size,
    ar.algo_name,
    ar.solve_time AS time_ms,
    ar.maze_id    -- should be NULL
FROM algo_run ar
JOIN player p ON ar.player_id = p.player_id
WHERE ar.maze_id IS NULL
ORDER BY ar.solve_time;


-- ── Q9: Personal Best vs Global Best comparison ──────────────────────────────
SELECT '=== Q9: Personal Best vs Global Best (per maze size) ===' AS "";

SELECT
    pb.maze_size,
    p.name                                  AS player,
    pb.steps                                AS personal_best_steps,
    gb.global_best_steps,
    pb.steps - gb.global_best_steps         AS gap_to_global_best
FROM persobest pb
JOIN player p ON pb.player_id = p.player_id
JOIN (
    SELECT maze_size, MIN(steps) AS global_best_steps
    FROM manual
    GROUP BY maze_size
) gb ON pb.maze_size = gb.maze_size
ORDER BY pb.maze_size, gap_to_global_best ASC;


-- ── Q10: Database health check — row counts + FK integrity ───────────────────
SELECT '=== Q10: Database Health — Row Counts per Table ===' AS "";

SELECT 'player'   AS tbl, COUNT(*) AS rows FROM player    UNION ALL
SELECT 'maze',            COUNT(*) FROM maze               UNION ALL
SELECT 'saves',           COUNT(*) FROM saves              UNION ALL
SELECT 'algo_run',        COUNT(*) FROM algo_run           UNION ALL
SELECT 'manual',          COUNT(*) FROM manual             UNION ALL
SELECT 'persobest',       COUNT(*) FROM persobest;

-- Foreign key integrity check (SQLite built-in)
SELECT '=== FK Integrity Check (empty = all good) ===' AS "";
PRAGMA foreign_key_check;

SELECT * FROM saves;

-- Index usage check
SELECT '=== Index List ===' AS "";
SELECT name, tbl_name FROM sqlite_master WHERE type = 'index' ORDER BY tbl_name;
