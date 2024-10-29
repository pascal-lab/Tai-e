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

package pascal.taie.frontend.newfrontend.bcir;

import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import pascal.taie.util.collection.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class FlattenExceptionTable {
    public enum ExceptionEntryType {
        END,
        START
    }

    public record ExceptionEntry(int pc, ExceptionEntryType type, int handlerPc) implements Comparable<ExceptionEntry> {
        public boolean isStart() {
            return type == ExceptionEntryType.START;
        }

        public boolean isEnd() {
            return type == ExceptionEntryType.END;
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

    List<ExceptionEntry> exceptionEntries;

    public FlattenExceptionTable(JSRInlinerAdapter source) {
        exceptionEntries = new ArrayList<>();
        for (TryCatchBlockNode node : source.tryCatchBlocks) {
            int start = getIndex(source, node.start);
            int end = getIndex(source, node.end);
            int handler = getIndex(source, node.handler);
            exceptionEntries.add(new ExceptionEntry(start, ExceptionEntryType.START, handler));
            exceptionEntries.add(new ExceptionEntry(end, ExceptionEntryType.END, handler));
        }
        // ascending order, put end before start
        Collections.sort(exceptionEntries);
    }

    public Pair<int[], Integer> buildExceptionSwitches() {
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

    List<Integer> currentHandlers = new ArrayList<>();
    private Queue<ExceptionEntry> entryQueue;

    public void init() {
        entryQueue = new ArrayDeque<>(exceptionEntries);
    }

    public boolean next(int pc) {
        boolean event = false;
        while (!entryQueue.isEmpty() && entryQueue.peek().pc() <= pc) {
            ExceptionEntry entry = entryQueue.poll();
            if (entry.isStart()) {
                currentHandlers.add(entry.handlerPc());
            } else {
                currentHandlers.remove((Integer) entry.handlerPc());
            }
            event = true;
        }
        return event;
    }

    private int getIndex(JSRInlinerAdapter source, LabelNode labelNode) {
        return source.instructions.indexOf(labelNode);
    }
}
