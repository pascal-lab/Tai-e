/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.frontend.java.ir;

import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class FlattenExceptionTable {

    /**
     * Marker type indicating whether an exception entry represents
     * the start or end of an exception handling region.
     */
    private enum ExceptionEntryType {
        END,
        START,
    }

    /**
     * Represents a boundary marker for an exception handling region.
     * Each try-catch block generates two entries: one START at the beginning
     * and one END at the end of the protected region.
     *
     * @param pc        the bytecode instruction index of this boundary
     * @param type      whether this is a START or END boundary
     * @param handlerPc the instruction index of the exception handler
     */
    private record ExceptionEntry(int pc, ExceptionEntryType type, int handlerPc)
            implements Comparable<ExceptionEntry> {

        /**
         * @return {@code true} if this entry marks the start of an exception region
         */
        private boolean isStart() {
            return type == ExceptionEntryType.START;
        }

        @Override
        public int compareTo(ExceptionEntry o) {
            if (pc() != o.pc()) {
                return pc - o.pc;
            } else {
                return o.type.ordinal() - type.ordinal();
            }
        }
    }

    /**
     * Sorted list of exception boundary entries (both START and END markers).
     * Sorted by PC in ascending order, with END entries before START entries
     * at the same PC position.
     */
    private final List<ExceptionEntry> exceptionEntries;

    /**
     * Constructs a flattened exception table from the given method's bytecode.
     * <p>
     * Extracts all try-catch blocks from the method, creates START and END
     * boundary markers for each, and sorts them for later processing.
     *
     * @param source the ASM method adapter containing the bytecode and exception table
     */
    FlattenExceptionTable(JSRInlinerAdapter source) {
        exceptionEntries = new ArrayList<>();
        for (TryCatchBlockNode node : source.tryCatchBlocks) {
            int start = getInsnIndex(source, node.start);
            int end = getInsnIndex(source, node.end);
            int handler = getInsnIndex(source, node.handler);
            exceptionEntries.add(new ExceptionEntry(start, ExceptionEntryType.START, handler));
            exceptionEntries.add(new ExceptionEntry(end, ExceptionEntryType.END, handler));
        }
        // ascending order, put end before start
        Collections.sort(exceptionEntries);
    }

    /**
     * Gets the instruction index for a given label node.
     */
    private static int getInsnIndex(JSRInlinerAdapter source, LabelNode labelNode) {
        return source.instructions.indexOf(labelNode);
    }

    Pair<int[], Integer> buildExceptionSwitches() {
        int[] result = new int[exceptionEntries.size()];
        int index = 0;
        int ends = 0;
        for (ExceptionEntry entry : exceptionEntries) {
            if (entry.isStart()) {
                if (ends == 0) {
                    result[index++] = entry.pc();
                }
                ends++;
            } else {
                ends--;
                if (ends == 0) {
                    result[index++] = entry.pc();
                }
            }
        }
        return new Pair<>(result, index);
    }
}
