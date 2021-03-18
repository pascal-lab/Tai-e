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

package pascal.taie.analysis.dataflow.clients.constprop;

import pascal.taie.analysis.dataflow.lattice.IFlowMap;
import soot.Local;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Data-flow value for constant propagation, which maps each variable to
 * a corresponding value (i.e., a product lattice of Local x Value).
 */
public class FlowMap extends LinkedHashMap<Local, Value>
        implements IFlowMap<Local, Value> {

    @Override
    public Value get(Object key) {
        return key instanceof Local
                ? getOrDefault(key, Value.getUndef())
                : null;
    }

    @Override
    public boolean update(Local key, Value value) {
        return !Objects.equals(put(key, value), value);
    }
}
