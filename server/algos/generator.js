function generate(rows, cols) {
    const cells = [];
    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            cells.push({ row: r, col: c, top: true, right: true, bottom: true, left: true, visited: false, isPath: false });
        }
    }

    function getCell(r, c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return null;
        return cells[r * cols + c];
    }

    function getUnvisitedNeighbors(cell) {
        const neighbors = [];
        const dirs = [
            { r: -1, c:  0, wall: 'top',    opp: 'bottom' },
            { r:  1, c:  0, wall: 'bottom', opp: 'top'    },
            { r:  0, c: -1, wall: 'left',   opp: 'right'  },
            { r:  0, c:  1, wall: 'right',  opp: 'left'   }
        ];
        for (const d of dirs) {
            const n = getCell(cell.row + d.r, cell.col + d.c);
            if (n && !n.visited) neighbors.push({ cell: n, wall: d.wall, oppWall: d.opp });
        }
        return neighbors;
    }

    const stack = [];
    const start = getCell(0, 0);
    start.visited = true;
    stack.push(start);

    while (stack.length > 0) {
        const current = stack[stack.length - 1];
        const neighbors = getUnvisitedNeighbors(current);

        if (neighbors.length === 0) {
            stack.pop();
        } else {
            const chosen = neighbors[Math.floor(Math.random() * neighbors.length)];
            current[chosen.wall]          = false;
            chosen.cell[chosen.oppWall]   = false;
            chosen.cell.visited           = true;
            stack.push(chosen.cell);
        }
    }

    cells.forEach(c => { c.visited = false; });

    return { rows, cols, cells };
}

module.exports = { generate };
