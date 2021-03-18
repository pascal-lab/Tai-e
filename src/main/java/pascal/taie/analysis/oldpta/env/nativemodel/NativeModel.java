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

package pascal.taie.analysis.oldpta.env.nativemodel;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.types.TypeManager;
import pascal.taie.analysis.oldpta.ir.PTAIR;

public interface NativeModel {

    static NativeModel getDefaultModel(
            ClassHierarchy hierarchy, TypeManager typeManager) {
        return new DefaultNativeModel(hierarchy, typeManager);
    }

    static NativeModel getDummyModel() {
        return (ir) -> {};
    }

    void process(PTAIR ir);
}
