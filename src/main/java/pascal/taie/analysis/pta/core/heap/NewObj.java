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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.Optional;

/**
 * Objects that are created by new statements.
 */
public class NewObj implements Obj {

    private final New allocSite;

    public NewObj(New allocSite) {
        this.allocSite = allocSite;
    }

    @Override
    public Type getType() {
        return allocSite.getRValue().getType();
    }

    @Override
    public New getAllocation() {
        return allocSite;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.of(allocSite.getContainer());
    }

    @Override
    public Type getContainerType() {
        return allocSite.getContainer()
                .getDeclaringClass()
                .getType();
    }

    @Override
    public String toString() {
        return String.format("NewObj{%s:%d(@L%d)/%s}",
                allocSite.getContainer(), allocSite.getIndex(),
                allocSite.getLineNumber(), allocSite.getRValue());
    }
}
