package com.maze.models;

public class Cell {
    private final int row;
    private final int col;
    private boolean topWall;
    private boolean rightWall;
    private boolean bottomWall;
    private boolean leftWall;
    private boolean visited;
    private boolean isPath;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.topWall = true;
        this.rightWall = true;
        this.bottomWall = true;
        this.leftWall = true;
        this.visited = false;
        this.isPath = false;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean hasTopWall() {
        return topWall;
    }

    public void setTopWall(boolean topWall) {
        this.topWall = topWall;
    }

    public boolean hasRightWall() {
        return rightWall;
    }

    public void setRightWall(boolean rightWall) {
        this.rightWall = rightWall;
    }

    public boolean hasBottomWall() {
        return bottomWall;
    }

    public void setBottomWall(boolean bottomWall) {
        this.bottomWall = bottomWall;
    }

    public boolean hasLeftWall() {
        return leftWall;
    }

    public void setLeftWall(boolean leftWall) {
        this.leftWall = leftWall;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public boolean isPath() {
        return isPath;
    }

    public void setPath(boolean isPath) {
        this.isPath = isPath;
    }

    public void removeWallBetween(Cell neighbor) {
        int rowDiff = this.row - neighbor.row;
        int colDiff = this.col - neighbor.col;

        if (rowDiff == 1) {
            this.topWall = false;
            neighbor.bottomWall = false;
        } else if (rowDiff == -1) {
            this.bottomWall = false;
            neighbor.topWall = false;
        } else if (colDiff == 1) {
            this.leftWall = false;
            neighbor.rightWall = false;
        } else if (colDiff == -1) {
            this.rightWall = false;
            neighbor.leftWall = false;
        }
    }
}
