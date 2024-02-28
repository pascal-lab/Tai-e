package pascal.taie.frontend.newfrontend;

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
