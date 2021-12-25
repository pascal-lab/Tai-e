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

/**
 * Each instance represents a taint flow from source to sink.
 */
public record TaintFlow(Invoke sourceCall, Invoke sinkCall, int index)
        implements Comparable<TaintFlow> {

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
    public String toString() {
        return String.format("TaintFlow{%s -> %s/%d}",
                CallGraphs.toString(sourceCall),
                CallGraphs.toString(sinkCall), index);
    }
}
