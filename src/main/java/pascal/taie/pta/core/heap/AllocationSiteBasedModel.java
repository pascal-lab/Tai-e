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

package pascal.taie.pta.core.heap;

import pascal.taie.java.TypeManager;
import pascal.taie.pta.ir.Allocation;
import pascal.taie.pta.ir.Obj;

import java.util.Map;

import static pascal.taie.util.CollectionUtils.newMap;

public class AllocationSiteBasedModel extends AbstractHeapModel {

    private final Map<Allocation, Obj> objects = newMap();

    public AllocationSiteBasedModel(TypeManager typeManager) {
        super(typeManager);
    }

    @Override
    protected Obj doGetObj(Allocation alloc) {
        return objects.computeIfAbsent(alloc, Allocation::getObject);
    }
}
