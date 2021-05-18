package pascal.taie.analysis.pta.plugin.Exception;

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.ir.stmt.Stmt;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class ExceptionWorkList {

    private final Queue<ExceptionWorkList.Entry> exceptionEntries = new LinkedList<>();

    boolean isEmpty() {
        return exceptionEntries.isEmpty();
    }

    void addExceptionEntry(CSMethod csMethod,
                           Stmt stmt,
                           Collection<CSObj> exceptions) {
        addPointerEntry(new ExceptionWorkList.Entry(csMethod, stmt, exceptions));
    }

    void addPointerEntry(ExceptionWorkList.Entry entry) {
        exceptionEntries.add(entry);
    }

    ExceptionWorkList.Entry pollPointerEntry() {
        return exceptionEntries.poll();
    }

    static class Entry {

        final CSMethod csMethod;

        final Stmt stmt;

        final Collection<CSObj> exceptions;

        public Entry(CSMethod csMethod,
                     Stmt stmt,
                     Collection<CSObj> exceptions) {
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
