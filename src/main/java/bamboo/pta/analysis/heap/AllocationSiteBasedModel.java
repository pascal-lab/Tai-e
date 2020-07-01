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

package bamboo.pta.analysis.heap;

import bamboo.pta.element.Obj;
import bamboo.pta.statement.Allocation;

import java.util.HashMap;
import java.util.Map;

public class AllocationSiteBasedModel implements HeapModel {

    private final Map<Allocation, Obj> objects = new HashMap<>();

    @Override
    public Obj getObj(Allocation alloc) {
        return objects.computeIfAbsent(alloc, Allocation::getObject);
    }
}
