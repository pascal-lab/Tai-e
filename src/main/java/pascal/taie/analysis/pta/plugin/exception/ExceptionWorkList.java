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

    static class Entry {

        final CSMethod csMethod;

        final Stmt stmt;

        final Collection<CSObj> exceptions;

        Entry(CSMethod csMethod, Stmt stmt, Collection<CSObj> exceptions) {
            this.csMethod = csMethod;
            this.stmt = stmt;
            this.exceptions = exceptions;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "csMethod=" + csMethod +
                    ", stmt=" + stmt +
                    ", exceptions=" + exceptions +
                    '}';
        }
    }
}
