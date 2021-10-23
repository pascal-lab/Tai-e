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

package pascal.taie.analysis.pta.ci;

import java.util.ArrayDeque;
import java.util.Queue;

class WorkList {

    private final Queue<Entry> pointerEntries = new ArrayDeque<>();

    void addPointerEntry(Pointer pointer, PointsToSet pointsToSet) {
        pointerEntries.add(new Entry(pointer, pointsToSet));
    }

    Entry pollPointerEntry() {
        return pointerEntries.poll();
    }

    boolean isEmpty() {
        return pointerEntries.isEmpty();
    }

    static class Entry {

        final Pointer pointer;

        final PointsToSet pointsToSet;

        Entry(Pointer pointer, PointsToSet pointsToSet) {
            this.pointer = pointer;
            this.pointsToSet = pointsToSet;
        }
    }
}
