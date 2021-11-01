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

package pascal.taie.analysis.pta.cs;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.pts.PointsToSet;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Represents work list in pointer analysis.
 */
class WorkList {

    private final Queue<Entry> entries = new ArrayDeque<>();

    /**
     * Adds an entry to the work list.
     */
    void addEntry(Pointer pointer, PointsToSet pointsToSet) {
        entries.add(new Entry(pointer, pointsToSet));
    }

    /**
     * Retrieves and removes an entry from this queue, or returns null
     * if this work list is empty.
     */
    Entry pollEntry() {
        return entries.poll();
    }

    /**
     * @return true if the work list is empty, otherwise false.
     */
    boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Represents entries in the work list.
     * Each entry consists of a pointer and a points-to set.
     */
    static class Entry {

        final Pointer pointer;

        final PointsToSet pointsToSet;

        Entry(Pointer pointer, PointsToSet pointsToSet) {
            this.pointer = pointer;
            this.pointsToSet = pointsToSet;
        }
    }
}
