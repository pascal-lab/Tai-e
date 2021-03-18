/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.oldpta.core.ci;

import pascal.taie.analysis.callgraph.Edge;
import pascal.taie.language.classes.JMethod;
import pascal.taie.analysis.oldpta.ir.CallSite;
import pascal.taie.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

class WorkList {

    private final Queue<Entry> pointerEntries = new LinkedList<>();

    private final Set<Edge<CallSite, JMethod>> callEdges = new LinkedHashSet<>();

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

    boolean hasCallEdges() {
        return !callEdges.isEmpty();
    }

    void addCallEdge(Edge<CallSite, JMethod> edge) {
        callEdges.add(edge);
    }

    Edge<CallSite, JMethod> pollCallEdge() {
        Edge<CallSite, JMethod> edge = CollectionUtils.getOne(callEdges);
        callEdges.remove(edge);
        return edge;
    }

    boolean isEmpty() {
        return pointerEntries.isEmpty() && callEdges.isEmpty();
    }

    static class Entry {

        final Pointer pointer;

        final PointsToSet pointsToSet;

        public Entry(Pointer pointer, PointsToSet pointsToSet) {
            this.pointer = pointer;
            this.pointsToSet = pointsToSet;
        }
    }
}
