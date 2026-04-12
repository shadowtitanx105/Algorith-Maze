package com.maze.models;

import java.util.ArrayList;
import java.util.List;

public class Maze {
    private final int rows;
    private final int cols;
    private final Cell[][] grid;
    private List<Cell> solution;

    public Maze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Cell[rows][cols];
        this.solution = new ArrayList<>();
        initializeGrid();
    }

    private void initializeGrid() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
    }

    public Cell getCell(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return grid[row][col];
        }
        return null;
    }

    public List<Cell> getUnvisitedNeighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();
        int row = cell.getRow();
        int col = cell.getCol();

        Cell top = getCell(row - 1, col);
        Cell right = getCell(row, col + 1);
        Cell bottom = getCell(row + 1, col);
        Cell left = getCell(row, col - 1);

        if (top != null && !top.isVisited()) neighbors.add(top);
        if (right != null && !right.isVisited()) neighbors.add(right);
        if (bottom != null && !bottom.isVisited()) neighbors.add(bottom);
        if (left != null && !left.isVisited()) neighbors.add(left);

        return neighbors;
    }

    public List<Cell> getAccessibleNeighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();
        int row = cell.getRow();
        int col = cell.getCol();

        if (!cell.hasTopWall()) {
            Cell top = getCell(row - 1, col);
            if (top != null) neighbors.add(top);
        }
        if (!cell.hasRightWall()) {
            Cell right = getCell(row, col + 1);
            if (right != null) neighbors.add(right);
        }
        if (!cell.hasBottomWall()) {
            Cell bottom = getCell(row + 1, col);
            if (bottom != null) neighbors.add(bottom);
        }
        if (!cell.hasLeftWall()) {
            Cell left = getCell(row, col - 1);
            if (left != null) neighbors.add(left);
        }

        return neighbors;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Cell[][] getGrid() {
        return grid;
    }

    public List<Cell> getSolution() {
        return solution;
    }

    public void setSolution(List<Cell> solution) {
        this.solution = solution;
    }

    public void resetVisited() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setVisited(false);
            }
        }
    }

    public void resetPath() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setPath(false);
            }
        }
    }
}
