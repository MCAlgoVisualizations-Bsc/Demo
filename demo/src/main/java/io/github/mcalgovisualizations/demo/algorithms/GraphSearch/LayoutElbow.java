package io.github.mcalgovisualizations.demo.algorithms.GraphSearch;

import io.github.mcalgovisualizations.prefab.Displays.NodeDisplay;
import io.github.mcalgovisualizations.visualization.layout.ILayout;
import io.github.mcalgovisualizations.visualization.renderer.LayoutResult;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

import java.util.*;

/**
 * Graph layout template that routes every edge as a single right-angle bend ("L" shape):
 * one straight run along Z, then one straight run along X.
 */
public class LayoutElbow implements ILayout<Node> {

    private static final int SCALE = 2;
    private static final int SPACING = 10 * SCALE;
    private final int gridCols;

    private final Block pathBlock = Block.DIRT_PATH;

    public LayoutElbow(int gridCols) {
        this.gridCols = gridCols;
    }

    @Override
    public LayoutResult[] compute(Node model, Pos origin, Instance instance) {
        if (model == null) return new LayoutResult[0];

        List<Node> nodes = collectNodes(model);
        if (nodes.isEmpty()) return new LayoutResult[0];

        int maxId = nodes.stream().mapToInt(Node::getID).max().orElse(-1);
        LayoutResult[] results = new LayoutResult[maxId + 1];

        double floorY = Math.floor(origin.y()) - 1;
        Pos floorOrigin = new Pos(origin.x(), floorY, origin.z());
        LayoutGrid grid = buildPositions(nodes, floorOrigin);
        Map<Integer, Pos> positions = grid.positions();

        clearArea(floorOrigin, instance, grid.width(), grid.height());
        placeNodes(nodes, instance, positions, results);
        drawEdges(nodes, instance, positions);

        return results;
    }

    private void placeNodes(List<Node> nodes, Instance instance, Map<Integer, Pos> positions, LayoutResult[] results) {
        for (Node node : nodes) {
            Pos pos = positions.get(node.getID());
            if (pos == null) continue;

            Block block = switch (node.getStatus()) {
                case Start -> { instance.setBlock(pos.add(0, 10, 0), Block.LIME_WOOL); yield Block.LIME_WOOL; }
                case End -> { instance.setBlock(pos.add(0, 10, 0), Block.RED_WOOL); yield Block.RED_WOOL; }
                default -> Block.WHITE_WOOL;
            };

            instance.setBlock(pos, block);
            results[node.getID()] = new LayoutResult(node.getValue(), pos, new NodeDisplay(String.valueOf(node.getValue()), pos));
        }
    }

    private void drawEdges(List<Node> nodes, Instance instance, Map<Integer, Pos> positions) {
        Set<String> drawnEdges = new HashSet<>();
        for (Node node : nodes) {
            Pos from = positions.get(node.getID());
            if (from == null) continue;
            for (Node neighbor : node.getNeighbors()) {
                Pos to = positions.get(neighbor.getID());
                if (to == null) continue;
                if (!drawnEdges.add(edgeKey(node.getID(), neighbor.getID()))) continue;
                drawElbow(from, to, instance);
            }
        }
    }

    private String edgeKey(int a, int b) {
        return a < b ? a + ":" + b : b + ":" + a;
    }

    /** Draws a right-angle path: straight along Z from {@code from}, then straight along X into {@code to}. */
    private void drawElbow(Pos from, Pos to, Instance instance) {
        Pos corner = new Pos(from.x(), from.y(), to.z());
        drawStraight(from, corner, instance);
        drawStraight(corner, to, instance);
    }

    private void drawStraight(Pos start, Pos end, Instance instance) {
        double dist = start.distance(end);
        int steps = (int) Math.ceil(dist);
        if (steps == 0) return;

        double dx = end.x() - start.x(), dz = end.z() - start.z();
        double length = Math.sqrt(dx * dx + dz * dz);
        double nx = length > 0 ? -dz / length : 0, nz = length > 0 ? dx / length : 0;

        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            double centerX = start.x() + dx * ratio, centerZ = start.z() + dz * ratio;
            for (int offset = -1; offset <= 1; offset++) {
                int px = (int) Math.round(centerX + (nx * offset));
                int pz = (int) Math.round(centerZ + (nz * offset));
                instance.setBlock(new Pos(px, start.y(), pz), pathBlock);
            }
        }
    }

    private void clearArea(Pos origin, Instance instance, int cols, int rows) {
        int margin = 12;
        for (int r = -margin; r < rows * SPACING + margin; r++) {
            for (int c = -margin; c < cols * SPACING + margin; c++) {
                instance.setBlock(origin.add(c, 0, r), Block.GRASS_BLOCK);
                for (int y = 1; y < 8; y++) instance.setBlock(origin.add(c, y, r), Block.AIR);
            }
        }
    }

    private List<Node> collectNodes(Node root) {
        List<Node> nodes = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (!visited.add(node.getID())) continue;
            nodes.add(node);
            for (Node neighbor : node.getNeighbors()) if (!visited.contains(neighbor.getID())) stack.push(neighbor);
        }
        return nodes;
    }

    private LayoutGrid buildPositions(List<Node> nodes, Pos origin) {
        List<Node> ordered = new ArrayList<>(nodes);
        ordered.sort(Comparator.comparingInt(Node::getValue).thenComparingInt(Node::getID));

        Map<Integer, int[]> gridPositions = new HashMap<>();
        Set<String> occupied = new HashSet<>();

        List<Node> pending = new ArrayList<>(ordered);
        if (!pending.isEmpty()) place(gridPositions, occupied, pending.removeFirst(), 0, 0);

        boolean progressed = true;
        while (!pending.isEmpty() && progressed) {
            progressed = false;
            Iterator<Node> it = pending.iterator();
            while (it.hasNext()) {
                Node node = it.next();
                int[] near = findNearPlacedSlot(node, gridPositions, occupied);
                if (near != null) {
                    place(gridPositions, occupied, node, near[0], near[1]);
                    it.remove();
                    progressed = true;
                }
            }
        }

        int i = 0;
        for (Node node : pending) {
            while (occupied.contains(key(i % gridCols, i / gridCols))) i++;
            place(gridPositions, occupied, node, i % gridCols, i / gridCols);
            i++;
        }

        normalizeGridCoordinates(gridPositions);
        int[] bounds = calculateBounds(gridPositions);
        Map<Integer, Pos> positions = new HashMap<>();
        for (Node node : ordered) {
            int[] gp = gridPositions.get(node.getID());
            if (gp != null) positions.put(node.getID(), origin.add(gp[0] * SPACING, 0, gp[1] * SPACING));
        }
        return new LayoutGrid(positions, bounds[0], bounds[1]);
    }

    private int[] findNearPlacedSlot(Node node, Map<Integer, int[]> gridPositions, Set<String> occupied) {
        List<int[]> anchors = new ArrayList<>();
        for (Node neighbor : node.getNeighbors()) {
            int[] pos = gridPositions.get(neighbor.getID());
            if (pos != null) anchors.add(pos);
        }
        if (anchors.isEmpty()) return null;
        for (int r = 1; r <= 8; r++) {
            for (int[] a : anchors) {
                int[][] cands = {{a[0]+r, a[1]}, {a[0], a[1]+r}, {a[0]-r, a[1]}, {a[0], a[1]-r}};
                for (int[] c : cands) if (!occupied.contains(key(c[0], c[1]))) return c;
            }
        }
        return null;
    }

    private void normalizeGridCoordinates(Map<Integer, int[]> gp) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (int[] p : gp.values()) { minX = Math.min(minX, p[0]); minZ = Math.min(minZ, p[1]); }
        if (minX == Integer.MAX_VALUE) return;
        for (int[] p : gp.values()) { p[0] -= minX; p[1] -= minZ; }
    }

    private int[] calculateBounds(Map<Integer, int[]> gp) {
        int mx = 0, mz = 0;
        for (int[] p : gp.values()) { mx = Math.max(mx, p[0]); mz = Math.max(mz, p[1]); }
        return new int[]{mx + 1, mz + 1};
    }

    private void place(Map<Integer, int[]> gp, Set<String> occ, Node n, int x, int z) {
        gp.put(n.getID(), new int[]{x, z});
        occ.add(key(x, z));
    }

    private String key(int x, int z) { return x + ":" + z; }

    private record LayoutGrid(Map<Integer, Pos> positions, int width, int height) {}
}
