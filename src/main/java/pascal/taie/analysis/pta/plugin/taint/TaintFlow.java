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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallGraphs;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.Hashes;

/**
 * Each instance represents a taint flow from source to sink.
 */
public class TaintFlow implements Comparable<TaintFlow> {

    /**
     * Invocation of the source method.
     */
    private final Invoke sourceCall;

    /**
     * Invocation of the sink method.
     */
    private final Invoke sinkCall;

    /**
     * Index of the sink argument.
     */
    private final int index;

    TaintFlow(Invoke sourceCall, Invoke sinkCall, int index) {
        this.sourceCall = sourceCall;
        this.sinkCall = sinkCall;
        this.index = index;
    }

    @Override
    public int compareTo(TaintFlow other) {
        int source = sourceCall.compareTo(other.sourceCall);
        if (source != 0) {
            return source;
        }
        int sink = sinkCall.compareTo(other.sinkCall);
        return sink != 0 ? sink : index - other.index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaintFlow taintFlow = (TaintFlow) o;
        return sourceCall.equals(taintFlow.sourceCall) &&
                sinkCall.equals(taintFlow.sinkCall) &&
                index == taintFlow.index;
    }

    @Override
    public int hashCode() {
        return Hashes.hash(sourceCall, sinkCall, index);
    }

    @Override
    public String toString() {
        return String.format("TaintFlow{%s -> %s/%d}",
                CallGraphs.toString(sourceCall),
                CallGraphs.toString(sinkCall), index);
    }
}
