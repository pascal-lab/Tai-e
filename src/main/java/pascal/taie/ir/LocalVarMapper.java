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

package pascal.taie.ir;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.ObjectIdMapper;

import java.util.Objects;

/**
 * Implementation of {@link ObjectIdMapper} for variables in a given ir.
 */
public class LocalVarMapper implements ObjectIdMapper<Var> {

    private final IR ir;

    public LocalVarMapper(IR ir) {
        Objects.requireNonNull(ir);
        this.ir = ir;
    }

    @Override
    public int getId(Var var) {
        return var.getIndex();
    }

    @Override
    public Var getObject(int id) {
        return ir.getVar(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LocalVarMapper mapper)) {
            return false;
        }
        return ir.equals(mapper.ir);
    }

    @Override
    public int hashCode() {
        return ir.hashCode();
    }
}
