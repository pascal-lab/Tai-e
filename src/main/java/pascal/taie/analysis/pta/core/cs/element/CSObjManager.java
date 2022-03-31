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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class CSObjManager implements Indexer<CSObj> {

    private final TwoKeyMap<Obj, Context, CSObj> objMap = Maps.newTwoKeyMap();

    /**
     * Counter for assigning unique indexes to CSObjs.
     */
    private int counter = 0;

    /**
     * Maps index to CSObj.
     */
    private final List<CSObj> objs = new ArrayList<>(65536);

    CSObj getCSObj(Context heapContext, Obj obj) {
        return objMap.computeIfAbsent(obj, heapContext, (o, c) -> {
            CSObj csObj = new CSObj(o, c, counter++);
            objs.add(csObj);
            return csObj;
        });
    }

    Collection<CSObj> getObjects() {
        return Collections.unmodifiableList(objs);
    }

    @Override
    public int getIndex(CSObj o) {
        return o.getIndex();
    }

    @Override
    public CSObj getObject(int index) {
        return objs.get(index);
    }
}
