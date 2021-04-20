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

package pascal.taie.analysis.oldpta.ir;

import pascal.taie.language.type.Type;

/**
 * All implementations of Obj should inherit this class.
 */
public abstract class AbstractObj implements Obj {

    /**
     * Type of this object.
     */
    protected final Type type;

    protected AbstractObj(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }
}
