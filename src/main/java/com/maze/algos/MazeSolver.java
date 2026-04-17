package com.maze.algos;

import com.maze.models.Cell;
import com.maze.models.Maze;

import java.util.*;

public class MazeSolver {

    public static class SolveResult {
        public final List<Cell> solution;
        public final List<Cell> visited;

        public SolveResult(List<Cell> solution, List<Cell> visited) {
            this.solution = solution;
            this.visited = visited;
        }
    }

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

    // ── A* ─────────────────────────────────────────────────────────────────

    public List<Cell> solve(Maze maze, Cell start, Cell end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Cell> closedSet = new HashSet<>();
        Map<Cell, Double> gScores = new HashMap<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        gScores.put(start, 0.0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (closedSet.contains(current.cell)) continue;

            if (current.cell == end) {
                return reconstructPath(current);
            }

            closedSet.add(current.cell);

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                if (closedSet.contains(neighbor)) continue;
                double tentativeGScore = current.gCost + 1;
                if (!gScores.containsKey(neighbor) || tentativeGScore < gScores.get(neighbor)) {
                    gScores.put(neighbor, tentativeGScore);
                    double h = heuristic(neighbor, end);
                    openSet.add(new Node(neighbor, current, tentativeGScore, h));
                }
            }
        }

        return new ArrayList<>();
    }

    public SolveResult solveWithVisited(Maze maze, Cell start, Cell end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Cell> closedSet = new HashSet<>();
        Map<Cell, Double> gScores = new HashMap<>();
        List<Cell> visitedOrder = new ArrayList<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        gScores.put(start, 0.0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (closedSet.contains(current.cell)) continue;
            closedSet.add(current.cell);
            visitedOrder.add(current.cell);

            if (current.cell == end) {
                return new SolveResult(reconstructPath(current), visitedOrder);
            }

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                if (closedSet.contains(neighbor)) continue;
                double tentativeGScore = current.gCost + 1;
                if (!gScores.containsKey(neighbor) || tentativeGScore < gScores.get(neighbor)) {
                    gScores.put(neighbor, tentativeGScore);
                    openSet.add(new Node(neighbor, current, tentativeGScore, heuristic(neighbor, end)));
                }
            }
        }

        return new SolveResult(new ArrayList<>(), visitedOrder);
    }

    // ── Dijkstra ───────────────────────────────────────────────────────────

    public List<Cell> solveDijkstra(Maze maze, Cell start, Cell end) {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        Map<Cell, Double> distances = new HashMap<>();

        Node startNode = new Node(start, null, 0, 0);
        queue.add(startNode);
        distances.put(start, 0.0);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.cell == end) {
                return reconstructPath(current);
            }

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                double newDist = current.gCost + 1;
                if (!distances.containsKey(neighbor) || newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    queue.add(new Node(neighbor, current, newDist, 0));
                }
            }
        }

        return new ArrayList<>();
    }

    public SolveResult solveDijkstraWithVisited(Maze maze, Cell start, Cell end) {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        Set<Cell> closedSet = new HashSet<>();
        Map<Cell, Double> distances = new HashMap<>();
        List<Cell> visitedOrder = new ArrayList<>();

        Node startNode = new Node(start, null, 0, 0);
        queue.add(startNode);
        distances.put(start, 0.0);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (closedSet.contains(current.cell)) continue;
            closedSet.add(current.cell);
            visitedOrder.add(current.cell);

            if (current.cell == end) {
                return new SolveResult(reconstructPath(current), visitedOrder);
            }

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                if (closedSet.contains(neighbor)) continue;
                double newDist = current.gCost + 1;
                if (!distances.containsKey(neighbor) || newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    queue.add(new Node(neighbor, current, newDist, 0));
                }
            }
        }

        return new SolveResult(new ArrayList<>(), visitedOrder);
    }

    // ── BFS ───────────────────────────────────────────────────────────────

    public List<Cell> solveBFS(Maze maze, Cell start, Cell end) {
        Queue<Node> queue = new LinkedList<>();
        Set<Cell> closedSet = new HashSet<>();
        Node startNode = new Node(start, null, 0, 0);
        queue.add(startNode);
        closedSet.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.cell == end) return reconstructPath(current);

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                if (!closedSet.contains(neighbor)) {
                    closedSet.add(neighbor);
                    queue.add(new Node(neighbor, current, current.gCost + 1, 0));
                }
            }
        }
        return new ArrayList<>();
    }

    public SolveResult solveBFSWithVisited(Maze maze, Cell start, Cell end) {
        Queue<Node> queue = new LinkedList<>();
        Set<Cell> closedSet = new HashSet<>();
        List<Cell> visitedOrder = new ArrayList<>();
        Node startNode = new Node(start, null, 0, 0);
        queue.add(startNode);
        closedSet.add(start);
        visitedOrder.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.cell == end) return new SolveResult(reconstructPath(current), visitedOrder);

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                if (!closedSet.contains(neighbor)) {
                    closedSet.add(neighbor);
                    visitedOrder.add(neighbor);
                    queue.add(new Node(neighbor, current, current.gCost + 1, 0));
                }
            }
        }
        return new SolveResult(new ArrayList<>(), visitedOrder);
    }

    // ── DFS ───────────────────────────────────────────────────────────────

    public List<Cell> solveDFS(Maze maze, Cell start, Cell end) {
        Stack<Node> stack = new Stack<>();
        Set<Cell> closedSet = new HashSet<>();
        Node startNode = new Node(start, null, 0, 0);
        stack.push(startNode);

        while (!stack.isEmpty()) {
            Node current = stack.pop();
            if (current.cell == end) return reconstructPath(current);

            if (!closedSet.contains(current.cell)) {
                closedSet.add(current.cell);
                for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                    if (!closedSet.contains(neighbor)) {
                        stack.push(new Node(neighbor, current, current.gCost + 1, 0));
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    public SolveResult solveDFSWithVisited(Maze maze, Cell start, Cell end) {
        Stack<Node> stack = new Stack<>();
        Set<Cell> closedSet = new HashSet<>();
        List<Cell> visitedOrder = new ArrayList<>();
        Node startNode = new Node(start, null, 0, 0);
        stack.push(startNode);

        while (!stack.isEmpty()) {
            Node current = stack.pop();
            if (current.cell == end) return new SolveResult(reconstructPath(current), visitedOrder);

            if (!closedSet.contains(current.cell)) {
                closedSet.add(current.cell);
                visitedOrder.add(current.cell);
                for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                    if (!closedSet.contains(neighbor)) {
                        stack.push(new Node(neighbor, current, current.gCost + 1, 0));
                    }
                }
            }
        }
        return new SolveResult(new ArrayList<>(), visitedOrder);
    }

    // ── Greedy Best-First ─────────────────────────────────────────────────

    public List<Cell> solveGreedy(Maze maze, Cell start, Cell end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Cell> closedSet = new HashSet<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.cell == end) return reconstructPath(current);

            if (closedSet.contains(current.cell)) continue;
            closedSet.add(current.cell);

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                if (!closedSet.contains(neighbor)) {
                    double h = heuristic(neighbor, end);
                    openSet.add(new Node(neighbor, current, 0, h));
                }
            }
        }
        return new ArrayList<>();
    }

    public SolveResult solveGreedyWithVisited(Maze maze, Cell start, Cell end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Cell> closedSet = new HashSet<>();
        List<Cell> visitedOrder = new ArrayList<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (closedSet.contains(current.cell)) continue;
            closedSet.add(current.cell);
            visitedOrder.add(current.cell);

            if (current.cell == end) return new SolveResult(reconstructPath(current), visitedOrder);

            for (Cell neighbor : maze.getAccessibleNeighbors(current.cell)) {
                if (!closedSet.contains(neighbor)) {
                    openSet.add(new Node(neighbor, current, 0, heuristic(neighbor, end)));
                }
            }
        }
        return new SolveResult(new ArrayList<>(), visitedOrder);
    }

    // ── Bidirectional BFS ─────────────────────────────────────────────────

    public List<Cell> solveBidirectional(Maze maze, Cell start, Cell end) {
        Queue<Node> queueA = new LinkedList<>();
        Queue<Node> queueB = new LinkedList<>();
        Map<Cell, Node> visitedA = new HashMap<>();
        Map<Cell, Node> visitedB = new HashMap<>();

        Node startNode = new Node(start, null, 0, 0);
        Node endNode = new Node(end, null, 0, 0);

        queueA.add(startNode);
        queueB.add(endNode);
        visitedA.put(start, startNode);
        visitedB.put(end, endNode);

        while (!queueA.isEmpty() && !queueB.isEmpty()) {
            Node currA = queueA.poll();
            for (Cell neighbor : maze.getAccessibleNeighbors(currA.cell)) {
                if (!visitedA.containsKey(neighbor)) {
                    Node nNodeA = new Node(neighbor, currA, currA.gCost + 1, 0);
                    visitedA.put(neighbor, nNodeA);
                    queueA.add(nNodeA);
                    if (visitedB.containsKey(neighbor))
                        return buildBidirectionalPath(nNodeA, visitedB.get(neighbor));
                }
            }

            Node currB = queueB.poll();
            for (Cell neighbor : maze.getAccessibleNeighbors(currB.cell)) {
                if (!visitedB.containsKey(neighbor)) {
                    Node nNodeB = new Node(neighbor, currB, currB.gCost + 1, 0);
                    visitedB.put(neighbor, nNodeB);
                    queueB.add(nNodeB);
                    if (visitedA.containsKey(neighbor))
                        return buildBidirectionalPath(visitedA.get(neighbor), nNodeB);
                }
            }
        }
        return new ArrayList<>();
    }

    public SolveResult solveBidirectionalWithVisited(Maze maze, Cell start, Cell end) {
        Queue<Node> queueA = new LinkedList<>();
        Queue<Node> queueB = new LinkedList<>();
        Map<Cell, Node> visitedA = new HashMap<>();
        Map<Cell, Node> visitedB = new HashMap<>();
        List<Cell> visitedOrder = new ArrayList<>();

        Node startNode = new Node(start, null, 0, 0);
        Node endNode = new Node(end, null, 0, 0);

        queueA.add(startNode);
        queueB.add(endNode);
        visitedA.put(start, startNode);
        visitedB.put(end, endNode);
        visitedOrder.add(start);
        visitedOrder.add(end);

        while (!queueA.isEmpty() && !queueB.isEmpty()) {
            Node currA = queueA.poll();
            for (Cell neighbor : maze.getAccessibleNeighbors(currA.cell)) {
                if (!visitedA.containsKey(neighbor)) {
                    Node nNodeA = new Node(neighbor, currA, currA.gCost + 1, 0);
                    visitedA.put(neighbor, nNodeA);
                    queueA.add(nNodeA);
                    visitedOrder.add(neighbor);
                    if (visitedB.containsKey(neighbor))
                        return new SolveResult(buildBidirectionalPath(nNodeA, visitedB.get(neighbor)), visitedOrder);
                }
            }

            Node currB = queueB.poll();
            for (Cell neighbor : maze.getAccessibleNeighbors(currB.cell)) {
                if (!visitedB.containsKey(neighbor)) {
                    Node nNodeB = new Node(neighbor, currB, currB.gCost + 1, 0);
                    visitedB.put(neighbor, nNodeB);
                    queueB.add(nNodeB);
                    visitedOrder.add(neighbor);
                    if (visitedA.containsKey(neighbor))
                        return new SolveResult(buildBidirectionalPath(visitedA.get(neighbor), nNodeB), visitedOrder);
                }
            }
        }
        return new SolveResult(new ArrayList<>(), visitedOrder);
    }

    // ── Shared Helpers ────────────────────────────────────────────────────

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

    private List<Cell> buildBidirectionalPath(Node nodeA, Node nodeB) {
        List<Cell> path = new ArrayList<>();
        Node current = nodeA;
        while (current != null) {
            path.add(0, current.cell);
            current = current.parent;
        }
        current = nodeB.parent;
        while (current != null) {
            path.add(current.cell);
            current = current.parent;
        }
        return path;
    }
}
