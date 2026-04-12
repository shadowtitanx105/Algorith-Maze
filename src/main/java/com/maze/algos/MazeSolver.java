package com.maze.algos;

import com.maze.models.Cell;
import com.maze.models.Maze;

import java.util.*;

public class MazeSolver {
    
    private static class Node implements Comparable<Node> {
        Cell cell;
        Node parent;
        double gCost;
        double hCost;
        
        Node(Cell cell, Node parent, double gCost, double hCost) {
            this.cell = cell;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
        }
        
        double fCost() {
            return gCost + hCost;
        }
        
        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fCost(), other.fCost());
        }
    }

    public List<Cell> solve(Maze maze, Cell start, Cell end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Cell> closedSet = new HashSet<>();
        Map<Cell, Double> gScores = new HashMap<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        gScores.put(start, 0.0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            
            if (current.cell == end) {
                return reconstructPath(current);
            }
            
            closedSet.add(current.cell);

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double tentativeGScore = current.gCost + 1;
                
                if (!gScores.containsKey(neighbor) || tentativeGScore < gScores.get(neighbor)) {
                    gScores.put(neighbor, tentativeGScore);
                    double h = heuristic(neighbor, end);
                    Node neighborNode = new Node(neighbor, current, tentativeGScore, h);
                    openSet.add(neighborNode);
                }
            }
        }

        return new ArrayList<>();
    }

    private double heuristic(Cell a, Cell b) {
        return Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getCol() - b.getCol());
    }

    private List<Cell> reconstructPath(Node endNode) {
        List<Cell> path = new ArrayList<>();
        Node current = endNode;
        
        while (current != null) {
            path.add(0, current.cell);
            current = current.parent;
        }
        
        return path;
    }

    public List<Cell> solveDijkstra(Maze maze, Cell start, Cell end) {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        Map<Cell, Double> distances = new HashMap<>();
        Map<Cell, Node> nodes = new HashMap<>();

        Node startNode = new Node(start, null, 0, 0);
        queue.add(startNode);
        distances.put(start, 0.0);
        nodes.put(start, startNode);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            if (current.cell == end) {
                return reconstructPath(current);
            }

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                double newDist = current.gCost + 1;
                
                if (!distances.containsKey(neighbor) || newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    Node neighborNode = new Node(neighbor, current, newDist, 0);
                    nodes.put(neighbor, neighborNode);
                    queue.add(neighborNode);
                }
            }
        }

        return new ArrayList<>();
    }
}
