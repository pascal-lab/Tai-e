package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.ir.stmt.Stmt;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

class ExceptionWorkList {

    private final Queue<Entry> exceptionEntries = new ArrayDeque<>();

    void addEntry(CSMethod csMethod, Stmt stmt, Collection<CSObj> exceptions) {
        exceptionEntries.add(new Entry(csMethod, stmt, exceptions));
    }

    Entry pollEntry() {
        return exceptionEntries.poll();
    }

    boolean isEmpty() {
        return exceptionEntries.isEmpty();
    }

    record Entry(CSMethod csMethod, Stmt stmt,
                 Collection<CSObj> exceptions) {
    }
}
