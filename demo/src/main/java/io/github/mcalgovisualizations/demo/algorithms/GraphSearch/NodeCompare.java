package io.github.mcalgovisualizations.demo.algorithms.GraphSearch;

import io.github.mcalgovisualizations.visualization.algorithm.IAlgorithmEvent;
import org.jetbrains.annotations.NotNull;

public record NodeCompare<V extends Comparable<V>>(Node x, Node y) implements IAlgorithmEvent {
    @Override
    public @NotNull String toString() {
        return "Compare(" + x.getValue() + " , " + y.getValue() + ")";
    }
}
