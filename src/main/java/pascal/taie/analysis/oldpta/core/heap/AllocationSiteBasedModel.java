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

package pascal.taie.analysis.oldpta.core.heap;

import pascal.taie.language.types.TypeManager;
import pascal.taie.analysis.oldpta.ir.Allocation;
import pascal.taie.analysis.oldpta.ir.Obj;

import java.util.Map;

import static pascal.taie.util.collection.CollectionUtils.newMap;

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
