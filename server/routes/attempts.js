const express = require('express');
const router  = express.Router();
const db      = require('../db');

router.post('/', async (req, res) => {
    try {
        const { playerName, mazeSize, duration, steps, mazeId = null } = req.body;
        if (!playerName || !mazeSize || duration == null || steps == null) {
            return res.status(400).json({ error: 'Missing required fields' });
        }

        const pool = await db.getPool();
        const playerId = await db.getOrCreatePlayer(playerName);

        await pool.query(
            'INSERT INTO manual (player_id, maze_id, maze_size, duration, steps, created_time) VALUES (?, ?, ?, ?, ?, NOW())',
            [playerId, mazeId, mazeSize, duration, steps]
        );

        // upsert persobest
        const [pbRows] = await pool.query('SELECT steps, duration FROM persobest WHERE player_id = ? AND maze_size = ?', [playerId, mazeSize]);
        if (pbRows.length === 0) {
            await pool.query('INSERT INTO persobest (player_id, maze_size, steps, duration, last_update) VALUES (?, ?, ?, ?, NOW())', [playerId, mazeSize, steps, duration]);
        } else {
            const exSteps = pbRows[0].steps;
            const exDur = pbRows[0].duration;
            if (steps < exSteps || (steps === exSteps && duration < exDur)) {
                await pool.query('UPDATE persobest SET steps = ?, duration = ?, last_update = NOW() WHERE player_id = ? AND maze_size = ?', [steps, duration, playerId, mazeSize]);
            }
        }

        res.json({ success: true });
    } catch (err) {
        console.error('Error in /api/attempts:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

router.get('/top', async (req, res) => {
    try {
        const limit = parseInt(req.query.limit) || 5;
        const pool = await db.getPool();
        const [rows] = await pool.query(`
            SELECT m.steps, m.duration, m.maze_size as mazeSize, p.name AS playerName
            FROM manual m
            JOIN player p ON m.player_id = p.player_id
            ORDER BY m.steps ASC, m.duration ASC
            LIMIT ?
        `, [limit]);

        res.json(rows);
    } catch (err) {
        console.error('Error in /api/attempts/top:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
