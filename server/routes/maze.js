const express    = require('express');
const router     = express.Router();
const generator  = require('../algos/generator');
const { solveAStar, solveDijkstra } = require('../algos/solver');

router.post('/generate', (req, res) => {
    const rows = parseInt(req.body.rows) || 20;
    const cols = parseInt(req.body.cols) || 20;
    if (rows < 2 || rows > 100 || cols < 2 || cols > 100) {
        return res.status(400).json({ error: 'Invalid dimensions' });
    }
    const maze = generator.generate(rows, cols);
    res.json(maze);
});

router.post('/solve', (req, res) => {
    const { maze, algoName } = req.body;
    if (!maze || !maze.cells) return res.status(400).json({ error: 'No maze provided' });

    const start = Date.now();
    const path  = algoName === 'dijkstra'
        ? solveDijkstra(maze)
        : solveAStar(maze);
    const solveTime = Date.now() - start;

    path.forEach(p => {
        const cell = maze.cells.find(c => c.row === p.row && c.col === p.col);
        if (cell) cell.isPath = true;
    });

    res.json({ solution: path, solveTime, pathLength: path.length });
});

module.exports = router;
