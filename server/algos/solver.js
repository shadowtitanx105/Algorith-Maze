function getAccessibleNeighbors(maze, cell) {
    const { rows, cols, cells } = maze;
    const neighbors = [];
    const dirs = [
        { r: -1, c:  0, wall: 'top'    },
        { r:  1, c:  0, wall: 'bottom' },
        { r:  0, c: -1, wall: 'left'   },
        { r:  0, c:  1, wall: 'right'  }
    ];
    for (const d of dirs) {
        const nr = cell.row + d.r;
        const nc = cell.col + d.c;
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
        if (!cell[d.wall]) {
            neighbors.push(cells[nr * cols + nc]);
        }
    }
    return neighbors;
}

function reconstructPath(cameFrom, endKey) {
    const path = [];
    let key = endKey;
    while (key !== null) {
        const [r, c] = key.split(',').map(Number);
        path.unshift({ row: r, col: c });
        key = cameFrom.get(key) || null;
    }
    return path;
}

function solveAStar(maze) {
    const { rows, cols, cells } = maze;
    const start = cells[0];
    const end   = cells[(rows - 1) * cols + (cols - 1)];

    function heuristic(cell) {
        return Math.abs(cell.row - end.row) + Math.abs(cell.col - end.col);
    }

    const openSet  = [];
    const gScore   = new Map();
    const fScore   = new Map();
    const cameFrom = new Map();

    const startKey = `${start.row},${start.col}`;
    const endKey   = `${end.row},${end.col}`;

    gScore.set(startKey, 0);
    fScore.set(startKey, heuristic(start));
    openSet.push({ key: startKey, f: heuristic(start), cell: start });
    cameFrom.set(startKey, null);

    while (openSet.length > 0) {
        openSet.sort((a, b) => a.f - b.f);
        const { key: currentKey, cell: current } = openSet.shift();

        if (currentKey === endKey) return reconstructPath(cameFrom, endKey);

        for (const neighbor of getAccessibleNeighbors(maze, current)) {
            const nKey     = `${neighbor.row},${neighbor.col}`;
            const tentG    = gScore.get(currentKey) + 1;
            if (!gScore.has(nKey) || tentG < gScore.get(nKey)) {
                cameFrom.set(nKey, currentKey);
                gScore.set(nKey, tentG);
                const f = tentG + heuristic(neighbor);
                fScore.set(nKey, f);
                openSet.push({ key: nKey, f, cell: neighbor });
            }
        }
    }
    return [];
}

function solveDijkstra(maze) {
    const { rows, cols, cells } = maze;
    const start = cells[0];
    const end   = cells[(rows - 1) * cols + (cols - 1)];

    const dist     = new Map();
    const cameFrom = new Map();
    const queue    = [];

    const startKey = `${start.row},${start.col}`;
    const endKey   = `${end.row},${end.col}`;

    dist.set(startKey, 0);
    cameFrom.set(startKey, null);
    queue.push({ key: startKey, d: 0, cell: start });

    while (queue.length > 0) {
        queue.sort((a, b) => a.d - b.d);
        const { key: currentKey, d: currentDist, cell: current } = queue.shift();

        if (currentKey === endKey) return reconstructPath(cameFrom, endKey);

        for (const neighbor of getAccessibleNeighbors(maze, current)) {
            const nKey  = `${neighbor.row},${neighbor.col}`;
            const newD  = currentDist + 1;
            if (!dist.has(nKey) || newD < dist.get(nKey)) {
                dist.set(nKey, newD);
                cameFrom.set(nKey, currentKey);
                queue.push({ key: nKey, d: newD, cell: neighbor });
            }
        }
    }
    return [];
}

module.exports = { solveAStar, solveDijkstra };
