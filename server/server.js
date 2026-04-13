const express = require('express');
const path    = require('path');
const db      = require('./db');

const mazeRoutes     = require('./routes/maze');
const scoresRoutes   = require('./routes/scores');
const attemptsRoutes = require('./routes/attempts');
const savesRoutes    = require('./routes/saves');

const app = express();
const PORT = process.env.PORT || 7007;

app.use(express.json({ limit: '10mb' }));

// Serve static frontend files
app.use(express.static(path.join(__dirname, '../src/main/resources/static')));

// Define API routes
app.use('/api', mazeRoutes);
app.use('/api/scores', scoresRoutes);
app.use('/api/attempts', attemptsRoutes);
app.use('/api/mazes', savesRoutes);

// Fallback to index.html
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, '../src/main/resources/static/index.html'));
});

// Start the server and initialize the DB
async function startServer() {
    try {
        await db.getPool(); // Initialize DB and create schema if not exists
        app.listen(PORT, () => {
            console.log(`🧩 Maze Master running on http://localhost:${PORT}`);
        });
    } catch (err) {
        console.error('Failed to start server:', err);
        process.exit(1);
    }
}

startServer();
