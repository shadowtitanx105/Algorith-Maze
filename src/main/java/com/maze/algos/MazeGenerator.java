package com.maze.algos;

import com.maze.models.Cell;
import com.maze.models.Maze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MazeGenerator {
    private final Random random;

    public MazeGenerator() {
        this.random = new Random();
    }

    public MazeGenerator(long seed) {
        this.random = new Random(seed);
    }

    public void generate(Maze maze) {
        Stack<Cell> stack = new Stack<>();
        Cell current = maze.getCell(0, 0);
        current.setVisited(true);
        stack.push(current);

        while (!stack.isEmpty()) {
            current = stack.peek();
            List<Cell> neighbors = maze.getUnvisitedNeighbors(current);

            if (!neighbors.isEmpty()) {
                Cell chosen = neighbors.get(random.nextInt(neighbors.size()));
                current.removeWallBetween(chosen);
                chosen.setVisited(true);
                stack.push(chosen);
            } else {
                stack.pop();
            }
        }

        maze.resetVisited();
    }

    public void makeBraid(Maze maze, int percent) {
        if (percent <= 0) return;
        int rows = maze.getRows();
        int cols = maze.getCols();
        List<int[]> walls = new ArrayList<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = maze.getCell(r, c);
                if (c + 1 < cols && cell.hasRightWall()) {
                    walls.add(new int[]{r, c, r, c + 1});
                }
                if (r + 1 < rows && cell.hasBottomWall()) {
                    walls.add(new int[]{r, c, r + 1, c});
                }
            }
        }

        Collections.shuffle(walls, random);
        int toRemove = (int) Math.round(walls.size() * percent / 100.0);
        for (int i = 0; i < toRemove && i < walls.size(); i++) {
            int[] w = walls.get(i);
            Cell a = maze.getCell(w[0], w[1]);
            Cell b = maze.getCell(w[2], w[3]);
            a.removeWallBetween(b);
        }
    }

    public void generateWithSeed(Maze maze, long seed) {
        Random seededRandom = new Random(seed);
        Stack<Cell> stack = new Stack<>();
        Cell current = maze.getCell(0, 0);
        current.setVisited(true);
        stack.push(current);

        while (!stack.isEmpty()) {
            current = stack.peek();
            List<Cell> neighbors = maze.getUnvisitedNeighbors(current);

            if (!neighbors.isEmpty()) {
                Cell chosen = neighbors.get(seededRandom.nextInt(neighbors.size()));
                current.removeWallBetween(chosen);
                chosen.setVisited(true);
                stack.push(chosen);
            } else {
                stack.pop();
            }
        }

        maze.resetVisited();
    }
}
