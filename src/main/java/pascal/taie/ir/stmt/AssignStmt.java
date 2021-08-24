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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Representation of assign statements.
 *
 * @param <L> type of lvalue.
 * @param <R> type of rvalue.
 */
public abstract class AssignStmt<L extends LValue, R extends RValue>
        extends DefinitionStmt<L, R> {

    private final L lvalue;

    private final R rvalue;

    public AssignStmt(L lvalue, R rvalue) {
        this.lvalue = lvalue;
        this.rvalue = rvalue;
    }

    @Override
    public @Nonnull
    L getLValue() {
        return lvalue;
    }

    @Override
    public R getRValue() {
        return rvalue;
    }

    @Override
    public Optional<Exp> getDef() {
        return Optional.of(lvalue);
    }

    @Override
    public List<Exp> getUses() {
        List<Exp> luses = lvalue.getUses();
        List<Exp> ruses = rvalue.getUses();
        List<Exp> uses = new ArrayList<>(luses.size() + ruses.size() + 1);
        uses.addAll(luses);
        uses.addAll(ruses);
        uses.add(rvalue);
        return uses;
    }

    @Override
    public String toString() {
        return lvalue + " = " + rvalue;
    }
}
