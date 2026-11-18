package ee.taltech.wiseones.geotank.server.domain.controller.strategy.pathfinding;

import java.util.*;

/**
 * A* pathfinding service for strategic navigation in the game world.
 */
public class PathfindingService {
    public static final float CENTER = 0.5f;
    public static final float DIAGONAL_MOVEMENT = 1.414f;
    public static final int MAX_ITERATION = 500;
    private final float gridSize;
    private final float arenaMinX;
    private final float arenaMaxX;
    private final float arenaMinY;
    private final float arenaMaxY;

    // Cache for recent path calculations
    private final Map<String, Path> pathCache = new LinkedHashMap<String, Path>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldestEntry) {
            return size() > 50; // LRU cache with max 50 entries
        }
    };

    public PathfindingService(float gridSize, float arenaMinX, float arenaMaxX, float arenaMinY, float arenaMaxY) {
        this.gridSize = gridSize;
        this.arenaMinX = arenaMinX;
        this.arenaMaxX = arenaMaxX;
        this.arenaMinY = arenaMinY;
        this.arenaMaxY = arenaMaxY;
    }

    /**
     * Find path using A* algorithm from start to goal position.
     *
     * @param startX starting X position
     * @param startY starting Y position
     * @param goalX target X position
     * @param goalY target Y position
     * @param obstacles set of obstacle positions to avoid (use obstacle bounds)
     * @return path as list of waypoints
     */
    public Path findPath(float startX, float startY, float goalX, float goalY, Set<Obstacle> obstacles) {
        // Create cache key for path
        String cacheKey = String.format("%.0f,%.0f->%.0f,%.0f", startX, startY, goalX, goalY);
        Path cachedPath = pathCache.get(cacheKey);
        if (cachedPath != null && !cachedPath.isStale()) {
            return cachedPath;
        }

        // Convert world coordinates to grid coordinates
        int startGridX = worldToGridX(startX);
        int startGridY = worldToGridY(startY);
        int goalGridX = worldToGridX(goalX);
        int goalGridY = worldToGridY(goalY);

        // Initialize A* structures
        Set<Node> openSet = new HashSet<>();
        Set<Node> closedSet = new HashSet<>();
        Map<Node, Node> cameFrom = new HashMap<>();

        Node startNode = new Node(startGridX, startGridY, 0, heuristic(startGridX, startGridY, goalGridX, goalGridY));
        openSet.add(startNode);

        Node current;
        int iterations = 0;
        // Prevent infinite loops

        while (!openSet.isEmpty() && iterations < MAX_ITERATION) {
            iterations++;

            // Find node with lowest f score
            current = openSet.stream().min(Comparator.comparingDouble(node -> node.f)).orElse(null);

            if (current.gridX == goalGridX && current.gridY == goalGridY) {
                // Path found, reconstruct it
                Path path = reconstructPath(cameFrom, current);
                pathCache.put(cacheKey, path);
                return path;
            }

            openSet.remove(current);
            closedSet.add(current);

            // Check all neighbors
            for (Node neighbor : getNeighbors(current)) {
                if (closedSet.contains(neighbor)) continue;
                if (isObstructed(gridToWorldX(neighbor.gridX), gridToWorldY(neighbor.gridY), obstacles)) continue;

                float tentativeG = current.g + neighbor.movementCost;

                Node inOpen = openSet.stream()
                        .filter(node -> node.gridX == neighbor.gridX && node.gridY == neighbor.gridY)
                        .findFirst()
                        .orElse(null);

                if (inOpen != null && tentativeG >= inOpen.g) continue;

                cameFrom.put(neighbor, current);
                neighbor.g = tentativeG;
                neighbor.h = heuristic(neighbor.gridX, neighbor.gridY, goalGridX, goalGridY);
                neighbor.f = neighbor.g + neighbor.h;

                openSet.remove(inOpen);
                openSet.add(neighbor);
            }
        }

        // No path found, return direct line
        List<Waypoint> directPath = new ArrayList<>();
        directPath.add(new Waypoint(goalX, goalY));
        return new Path(directPath);
    }

    /**
     * Calculate heuristic distance (Manhattan distance on grid)
     */
    private float heuristic(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + (float) Math.abs(y1 - y2);
    }

    /**
     * Get valid neighboring nodes for pathfinding
     */
    private List<Node> getNeighbors(Node current) {
        List<Node> neighbors = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

        for (int[] dir : directions) {
            int newX = current.gridX + dir[0];
            int newY = current.gridY + dir[1];

            if (isValidGridPosition(newX, newY)) {
                float movementCost = (dir[0] != 0 && dir[1] != 0) ? DIAGONAL_MOVEMENT : 1.0f; // Diagonal vs cardinal
                neighbors.add(new Node(newX, newY, 0, 0, movementCost));
            }
        }

        return neighbors;
    }

    /**
     * Check if a grid position is within map bounds
     */
    private boolean isValidGridPosition(int gridX, int gridY) {
        int maxGridX = (int) ((arenaMaxX - arenaMinX) / gridSize);
        int maxGridY = (int) ((arenaMaxY - arenaMinY) / gridSize);
        return gridX >= 0 && gridX < maxGridX && gridY >= 0 && gridY < maxGridY;
    }

    /**
     * Check if a world position is obstructed by obstacles
     */
    private boolean isObstructed(float worldX, float worldY, Set<Obstacle> obstacles) {
        if (obstacles == null || obstacles.isEmpty()) return false;
        for (Obstacle obstacle : obstacles) {
            if (obstacle.contains(worldX, worldY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reconstruct path from A* cameFrom map
     */
    private Path reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<Waypoint> waypoints = new ArrayList<>();
        waypoints.add(new Waypoint(gridToWorldX(current.gridX), gridToWorldY(current.gridY)));

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            waypoints.add(new Waypoint(gridToWorldX(current.gridX), gridToWorldY(current.gridY)));
        }

        Collections.reverse(waypoints);
        return new Path(waypoints);
    }

    /**
     * Convert world X coordinate to grid coordinate
     */
    private int worldToGridX(float worldX) {
        return (int) ((worldX - arenaMinX) / gridSize);
    }

    /**
     * Convert world Y coordinate to grid coordinate
     */
    private int worldToGridY(float worldY) {
        return (int) ((worldY - arenaMinY) / gridSize);
    }

    /**
     * Convert grid X coordinate to world coordinate (center of grid cell)
     */
    private float gridToWorldX(int gridX) {
        return arenaMinX + (gridX + CENTER) * gridSize;
    }

    /**
     * Convert grid Y coordinate to world coordinate (center of grid cell)
     */
    private float gridToWorldY(int gridY) {
        return arenaMinY + (gridY + CENTER) * gridSize;
    }

    /**
     * A* node for pathfinding
     */
    private static class Node {
        int gridX;
        int gridY;
        float g; // Cost from start
        float h; // Heuristic to goal
        float f; // Total score (g + h)
        float movementCost;

        Node(int gridX, int gridY, float g, float h) {
            this(gridX, gridY, g, h, 1.0f);
        }

        Node(int gridX, int gridY, float g, float h, float movementCost) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.movementCost = movementCost;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Node node = (Node) obj;
            return gridX == node.gridX && gridY == node.gridY;
        }

        @Override
        public int hashCode() {
            return Objects.hash(gridX, gridY);
        }
    }

    /**
     * Represents an obstacle in the pathfinding grid
     */
    public interface Obstacle {
        boolean contains(float x, float y);
    }

    /**
     * A waypoint in a path
     */
    public static class Waypoint {
        public final float x;
        public final float y;

        public Waypoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * A complete path from start to goal
     */
    public static class Path {
        public final List<Waypoint> waypoints;
        private final long createdTime;

        public Path() {
            this(new ArrayList<>());
        }

        public Path(List<Waypoint> waypoints) {
            this.waypoints = waypoints;
            this.createdTime = System.currentTimeMillis();
        }

        public boolean isEmpty() {
            return waypoints == null || waypoints.isEmpty();
        }

        public boolean isStale() {
            // Paths are valid for 1 second before recalculation
            return System.currentTimeMillis() - createdTime > 1000;
        }
    }
}
