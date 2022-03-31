/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.pts.PointsToSet;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Represented work list in pointer analysis. This class actually contains
 * two work lists, one for pointer entries and one for call edges.
 */
final class WorkList {

    private final Map<Pointer, PointsToSet> pointerEntries = new LinkedHashMap<>();

    private final Queue<Edge<CSCallSite, CSMethod>> callEdges = new ArrayDeque<>();

    boolean hasPointerEntries() {
        return !pointerEntries.isEmpty();
    }

    void addPointerEntry(Pointer pointer, PointsToSet pointsToSet) {
        pointerEntries.computeIfAbsent(pointer, unused -> pointsToSet.copy())
                .addAll(pointsToSet);
    }

    Entry pollPointerEntry() {
        if (pointerEntries.isEmpty()) {
            throw new NoSuchElementException();
        }
        var it = pointerEntries.entrySet().iterator();
        var e = it.next();
        it.remove();
        return new Entry(e.getKey(), e.getValue());
    }

    boolean hasCallEdges() {
        return !callEdges.isEmpty();
    }

    void addCallEdge(Edge<CSCallSite, CSMethod> edge) {
        callEdges.add(edge);
    }

    Edge<CSCallSite, CSMethod> pollCallEdge() {
        return callEdges.poll();
    }

    boolean isEmpty() {
        return pointerEntries.isEmpty() && callEdges.isEmpty();
    }

    record Entry(Pointer pointer, PointsToSet pointsToSet) {
    }
}
