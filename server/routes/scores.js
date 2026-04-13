const express = require('express');
const router  = express.Router();
const db      = require('../db');

router.post('/', async (req, res) => {
    try {
        const { playerName, mazeSize, algoName = 'astar', solveTime, pathLength = 0, mazeId = null } = req.body;
        if (!playerName || !mazeSize || solveTime == null) {
            return res.status(400).json({ error: 'Missing required fields' });
        }

        const pool = await db.getPool();
        const playerId = await db.getOrCreatePlayer(playerName);

        await pool.query(
            'INSERT INTO algo_run (player_id, maze_id, maze_size, algo_name, solve_time, path_length, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())',
            [playerId, mazeId, mazeSize, algoName, solveTime, pathLength]
        );

        res.json({ success: true });
    } catch (err) {
        console.error('Error in /api/scores:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

router.get('/top', async (req, res) => {
    try {
        const limit = parseInt(req.query.limit) || 5;
        const pool = await db.getPool();
        const [rows] = await pool.query(`
            SELECT ar.solve_time as solveTime, ar.maze_size as mazeSize, ar.algo_name as algoName, ar.path_length as pathLength,
                   p.name AS playerName
            FROM algo_run ar
            JOIN player p ON ar.player_id = p.player_id
            ORDER BY ar.solve_time ASC
            LIMIT ?
        `, [limit]);

        res.json(rows);
    } catch (err) {
        console.error('Error in /api/scores/top:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
