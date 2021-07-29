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

import pascal.taie.analysis.graph.callgraph.CGUtils;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.HashUtils;

/**
 * Each instance represents a taint flow from source to sink.
 */
class TaintFlow implements Comparable<TaintFlow> {

    private final Invoke sourceCall;

    private final Invoke sinkCall;

    TaintFlow(Invoke sourceCall, Invoke sinkCall) {
        this.sourceCall = sourceCall;
        this.sinkCall = sinkCall;
    }

    @Override
    public int compareTo(TaintFlow other) {
        int source = sourceCall.compareTo(other.sourceCall);
        return source != 0 ? source : sinkCall.compareTo(other.sinkCall);
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
                sinkCall.equals(taintFlow.sinkCall);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(sourceCall, sinkCall);
    }

    @Override
    public String toString() {
        return String.format("TaintFlow{%s -> %s}",
                CGUtils.toString(sourceCall),
                CGUtils.toString(sinkCall));
    }
}
