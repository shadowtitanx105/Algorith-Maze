async function fetchAndDrawMaze() {
    // 1. Listen/Fetch: Ask Java for the solved maze
    const response = await fetch('/api/solve');
    const data = await response.json();
    const grid = data.maze;

    // 2. JS Draws: Use Canvas to render the 2D array
    const canvas = document.getElementById('mazeCanvas');
    const ctx = canvas.getContext('2d');
    const cellSize = 50;

    grid.forEach((row, y) => {
        row.forEach((cell, x) => {
            ctx.fillStyle = (cell === 1) ? "#2c3e50" : "#ecf0f1"; // Wall vs Path
            ctx.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
            ctx.strokeRect(x * cellSize, y * cellSize, cellSize, cellSize);
        });
    });
    console.log("Java says: " + data.status);
}

// Trigger the save functionality
function saveScore() {
    fetch('/api/save', { method: 'POST' })
        .then(res => res.text())
        .then(msg => alert(msg));
}

fetchAndDrawMaze();
