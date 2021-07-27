/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.MapUtils;

import java.util.Map;

class TaintFactory {

    private static final String TAINT_DESC = "TaintObj";

    private Map<Invoke, Map<Type, Obj>> taints = MapUtils.newHybridMap();

    Obj getTaint(Invoke source, Type type) {
        Obj taint = MapUtils.getMapMap(taints, source, type);
        if (taint == null) {
            taint = newTaint(source, type);
            MapUtils.addToMapMap(taints, source, type, taint);
        }
        return taint;
    }

    private Obj newTaint(Invoke source, Type type) {
        return new MockObj(TAINT_DESC, source, type);
    }
}
