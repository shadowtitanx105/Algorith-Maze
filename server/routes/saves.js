const express = require('express');
const router  = express.Router();
const db      = require('../db');

router.post('/save', async (req, res) => {
    try {
        const { playerName, label = 'Untitled Maze', mazeData } = req.body;
        if (!playerName || !mazeData) {
            return res.status(400).json({ error: 'Missing requried fields' });
        }

        const pool = await db.getPool();
        const playerId = await db.getOrCreatePlayer(playerName);

        const rows = mazeData.rows;
        const cols = mazeData.cols;
        const gridJson = JSON.stringify(mazeData);

        const [insertMazeResult] = await pool.query(
            'INSERT INTO maze (rows, cols, grid_data, created_at) VALUES (?, ?, ?, NOW())',
            [rows, cols, gridJson]
        );
        const mazeId = insertMazeResult.insertId;

        await pool.query(
            'INSERT INTO saves (player_id, maze_id, label, saved_at) VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE label = ?, saved_at = NOW()',
            [playerId, mazeId, label, label]
        );

        res.json({ success: true, mazeId: mazeId });
    } catch (err) {
        console.error('Error in /api/mazes/save:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

router.get('/saved', async (req, res) => {
    try {
        const pool = await db.getPool();
        const [rows] = await pool.query(`
            SELECT m.maze_id as mazeId, s.label, m.rows, m.cols, s.saved_at as savedAt, p.name AS playerName
            FROM saves s
            JOIN maze   m ON s.maze_id   = m.maze_id
            JOIN player p ON s.player_id = p.player_id
            ORDER BY s.saved_at DESC
        `);
        res.json(rows);
    } catch (err) {
        console.error('Error in /api/mazes/saved:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

router.get('/:id', async (req, res) => {
    try {
        const mazeId = parseInt(req.params.id);
        const pool = await db.getPool();
        const [rows] = await pool.query('SELECT grid_data FROM maze WHERE maze_id = ?', [mazeId]);

        if (rows.length === 0) {
            return res.status(404).json({ error: 'Maze not found' });
        }

        res.json(JSON.parse(rows[0].grid_data));
    } catch (err) {
        console.error('Error in /api/mazes/:id:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
