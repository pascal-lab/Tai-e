/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.solver;

import bamboo.callgraph.Edge;
import bamboo.pta.analysis.data.CSCallSite;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.Pointer;
import bamboo.pta.set.PointsToSet;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

class WorkList {

    private final Queue<Entry> pointerEntries = new LinkedList<>();

    private final Set<Edge<CSCallSite, CSMethod>> callEdges = new LinkedHashSet<>();

    boolean hasPointerEntries() {
        return !pointerEntries.isEmpty();
    }

    void addPointerEntry(Pointer pointer, PointsToSet pointsToSet) {
        addPointerEntry(new Entry(pointer, pointsToSet));
    }

    void addPointerEntry(Entry entry) {
        pointerEntries.add(entry);
    }

    Entry pollPointerEntry() {
        return pointerEntries.poll();
    }

    class Entry {

        final Pointer pointer;

        final PointsToSet pointsToSet;

        public Entry(Pointer pointer, PointsToSet pointsToSet) {
            this.pointer = pointer;
            this.pointsToSet = pointsToSet;
        }
    }

    boolean hasCallEdges() {
        return !callEdges.isEmpty();
    }

    void addCallEdge(Edge<CSCallSite, CSMethod> edge) {
        callEdges.add(edge);
    }

    Edge<CSCallSite, CSMethod> pollCallEdge() {
        Edge<CSCallSite, CSMethod> edge = callEdges.iterator().next();
        callEdges.remove(edge);
        return edge;
    }

    boolean isEmpty() {
        return pointerEntries.isEmpty() && callEdges.isEmpty();
    }
}
