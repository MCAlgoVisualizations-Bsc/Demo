package io.github.mcalgovisualizations.demo.algorithms.GraphSearch;

import io.github.mcalgovisualizations.visualization.algorithm.IAlgorithmEvent;

import java.util.Map;

public record PathFoundEvent(
        Node startNode,
        Node targetNode,
        Map<Integer, Node> parents
) implements IAlgorithmEvent {}
