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

package panda.pta.core.heap;

import panda.pta.core.ProgramManager;
import panda.pta.element.Obj;
import panda.pta.statement.Allocation;

import java.util.HashMap;
import java.util.Map;

public class AllocationSiteBasedModel extends AbstractHeapModel {

    private final Map<Allocation, Obj> objects = new HashMap<>();

    public AllocationSiteBasedModel(ProgramManager pm) {
        super(pm);
    }

    @Override
    protected Obj doGetObj(Allocation alloc) {
        return objects.computeIfAbsent(alloc, Allocation::getObject);
    }
}
