package io.github.mcalgovisualizations.demo.algorithms.GraphSearch;

import io.github.mcalgovisualizations.prefab.Displays.NodeDisplay;
import io.github.mcalgovisualizations.visualization.layout.ILayout;
import io.github.mcalgovisualizations.visualization.renderer.LayoutResult;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

import java.util.*;

/**
 * Graph layout template that routes every edge as a right-angle bend, like {@link LayoutElbow},
 * and marks any point where two independent edges actually cross with a distinct block so it
 * reads as a four-way ("+") junction.
 */
public class LayoutCross implements ILayout<Node> {

    private static final int SCALE = 2;
    private static final int SPACING = 10 * SCALE;
    private final int gridCols;

    private final Block pathBlock = Block.DIRT_PATH;
    private final Block crossingBlock = Block.POLISHED_ANDESITE;

    public LayoutCross(int gridCols) {
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
        LayoutGrid grid = buildPositions(model, nodes, floorOrigin);
        Map<Integer, Pos> positions = grid.positions();

        clearArea(floorOrigin, instance, grid.width(), grid.height());

        List<Segment> horizontals = new ArrayList<>();
        List<Segment> verticals = new ArrayList<>();
        drawEdges(nodes, instance, positions, horizontals, verticals);
        markCrossings(horizontals, verticals, instance, floorOrigin.y());

        // Node blocks are placed last so they always sit on top of any path/crossing block underneath them.
        placeNodes(nodes, instance, positions, results);

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

    private void drawEdges(List<Node> nodes, Instance instance, Map<Integer, Pos> positions,
                            List<Segment> horizontals, List<Segment> verticals) {
        Set<String> drawnEdges = new HashSet<>();
        int edgeId = 0;
        for (Node node : nodes) {
            Pos from = positions.get(node.getID());
            if (from == null) continue;
            for (Node neighbor : node.getNeighbors()) {
                Pos to = positions.get(neighbor.getID());
                if (to == null) continue;
                if (!drawnEdges.add(edgeKey(node.getID(), neighbor.getID()))) continue;
                drawElbow(from, to, instance, edgeId++, horizontals, verticals);
            }
        }
    }

    /** A straight run of one edge's elbow, along a fixed X (vertical) or fixed Z (horizontal). */
    private record Segment(int edgeId, double fixed, double min, double max) {}

    /** Marks every point where two different edges' segments cross with a distinct block. */
    private void markCrossings(List<Segment> horizontals, List<Segment> verticals, Instance instance, double y) {
        for (Segment h : horizontals) {
            for (Segment v : verticals) {
                if (h.edgeId() == v.edgeId()) continue;
                if (v.fixed() < h.min() || v.fixed() > h.max()) continue;
                if (h.fixed() < v.min() || h.fixed() > v.max()) continue;
                instance.setBlock(new Pos(v.fixed(), y, h.fixed()), crossingBlock);
            }
        }
    }

    private String edgeKey(int a, int b) {
        return a < b ? a + ":" + b : b + ":" + a;
    }

    /** Draws a right-angle path: straight along Z from {@code from}, then straight along X into {@code to}. */
    private void drawElbow(Pos from, Pos to, Instance instance, int edgeId, List<Segment> horizontals, List<Segment> verticals) {
        Pos corner = new Pos(from.x(), from.y(), to.z());
        drawStraight(from, corner, instance);
        drawStraight(corner, to, instance);
        verticals.add(new Segment(edgeId, from.x(), Math.min(from.z(), corner.z()), Math.max(from.z(), corner.z())));
        horizontals.add(new Segment(edgeId, to.z(), Math.min(corner.x(), to.x()), Math.max(corner.x(), to.x())));
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

    /**
     * Places nodes by BFS depth (row) and position within that depth level (column), so any
     * branching graph spreads across both axes and its elbow edges actually have a chance to
     * cross — a nearest-open-slot packing tends to collapse small or chain-shaped graphs into a
     * single row. A level wider than {@code gridCols} wraps onto additional full-spacing rows
     * (never a half-spacing sub-row, which used to overlap adjacent segments into a stray loop).
     * When every node carries an explicit {@link Node#hasGridPosition()}, those exact coordinates
     * are used instead — BFS depth can't tell "this neighbor continues the spine" from "this one
     * branches sideways" on an undirected graph, so a hand-designed shape needs explicit hints.
     */
    private LayoutGrid buildPositions(Node root, List<Node> nodes, Pos origin) {
        if (nodes.stream().allMatch(Node::hasGridPosition)) {
            return buildPinnedPositions(nodes, origin);
        }

        List<List<Node>> levels = new ArrayList<>();
        Map<Integer, Integer> depthById = new HashMap<>();
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(root);
        depthById.put(root.getID(), 0);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int depth = depthById.get(current.getID());
            while (levels.size() <= depth) levels.add(new ArrayList<>());
            levels.get(depth).add(current);

            List<Node> neighbors = new ArrayList<>(current.getNeighbors());
            neighbors.sort(Comparator.comparingInt(Node::getValue).thenComparingInt(Node::getID));
            for (Node neighbor : neighbors) {
                if (!depthById.containsKey(neighbor.getID())) {
                    depthById.put(neighbor.getID(), depth + 1);
                    queue.add(neighbor);
                }
            }
        }

        Map<Integer, Pos> positions = new HashMap<>();
        int maxCols = 1;
        int rowsUsed = 0;
        for (List<Node> level : levels) {
            int wrapRows = (int) Math.ceil(level.size() / (double) gridCols);
            for (int idx = 0; idx < level.size(); idx++) {
                int col = idx % gridCols;
                int rowInLevel = idx / gridCols;
                maxCols = Math.max(maxCols, Math.min(level.size(), gridCols));
                Pos pos = origin.add(col * SPACING, 0, (rowsUsed + rowInLevel) * SPACING);
                positions.put(level.get(idx).getID(), pos);
            }
            rowsUsed += wrapRows;
        }
        return new LayoutGrid(positions, maxCols, rowsUsed);
    }

    private LayoutGrid buildPinnedPositions(List<Node> nodes, Pos origin) {
        int minX = nodes.stream().mapToInt(Node::getGridX).min().orElse(0);
        int minZ = nodes.stream().mapToInt(Node::getGridZ).min().orElse(0);

        Map<Integer, Pos> positions = new HashMap<>();
        int maxX = 0, maxZ = 0;
        for (Node node : nodes) {
            int x = node.getGridX() - minX;
            int z = node.getGridZ() - minZ;
            maxX = Math.max(maxX, x);
            maxZ = Math.max(maxZ, z);
            positions.put(node.getID(), origin.add(x * SPACING, 0, z * SPACING));
        }
        return new LayoutGrid(positions, maxX + 1, maxZ + 1);
    }

    private record LayoutGrid(Map<Integer, Pos> positions, int width, int height) {}
}
