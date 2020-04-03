/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.dataflow.analysis.constprop;

import soot.Local;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Data-flow value for constant propagation, which maps each variable to
 * a corresponding value (i.e., a product lattice of Local x Value).
 */
public class FlowMap extends LinkedHashMap<Local, Value>
        implements bamboo.dataflow.lattice.FlowMap<Local, Value> {

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
