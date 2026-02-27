async function fetchAndDrawMaze() {
    const response = await fetch('/api/solve');
    const data = await response.json();
    const grid = data.maze;
    
    const canvas = document.getElementById('mazeCanvas');
    const ctx = canvas.getContext('2d');

    // Configuration
    const rows = data.rows;
    const cols = data.cols;
    const cellSize = canvas.width / cols; // Dynamic sizing!

    // Clear canvas before drawing
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    grid.forEach((row, y) => {
        row.forEach((cell, x) => {
            // Pick colors: Dark for walls, Light for paths
            ctx.fillStyle = (cell === 1) ? "#2c3e50" : "#ffffff"; 
            
            // Draw the box
            ctx.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
            
            // Optional: Draw very faint grid lines
            ctx.strokeStyle = "#dfe6e9";
            ctx.lineWidth = 0.5;
            ctx.strokeRect(x * cellSize, y * cellSize, cellSize, cellSize);
        });
    });
}
// Trigger the save functionality
function saveScore() {
    fetch('/api/save', { method: 'POST' })
        .then(res => res.text())
        .then(msg => alert(msg));
}

fetchAndDrawMaze();
