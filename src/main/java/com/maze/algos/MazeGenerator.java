package com.maze.algos;

import com.maze.models.Cell;
import com.maze.models.Maze;

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
