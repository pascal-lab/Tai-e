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
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;

import java.util.Map;

/**
 * Manages taint objects.
 */
class TaintManager {

    private static final String TAINT_DESC = "TaintObj";

    private final Map<Invoke, Map<Type, Obj>> taints = Maps.newHybridMap();

    /**
     * Makes a taint object for given source and type.
     *
     * @param source invocation to the source method, i.e., source call
     * @param type   type of the taint object
     * @return the taint object for given source and type.
     */
    Obj makeTaint(Invoke source, Type type) {
        Obj taint = Maps.getMapMap(taints, source, type);
        if (taint == null) {
            taint = new MockObj(TAINT_DESC, source, type);
            Maps.addToMapMap(taints, source, type, taint);
        }
        return taint;
    }

    /**
     * @return true if given obj represents a taint object, otherwise false.
     */
    boolean isTaint(Obj obj) {
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescription().equals(TAINT_DESC);
    }

    /**
     * @return the source call of given taint object.
     * @throws AnalysisException if given object is not a taint object.
     */
    Invoke getSourceCall(Obj obj) {
        if (isTaint(obj)) {
            return (Invoke) obj.getAllocation();
        }
        throw new AnalysisException(obj + " is not a taint object");
    }
}
