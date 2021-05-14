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
import pascal.taie.ir.exp.Var;

import java.util.List;
import java.util.Optional;

/**
 * Representation of monitorenter/monitorexit instruction.
 */
public class Monitor extends AbstractStmt {

    // TODO: hide Op? To achieve this, we can replace the constructors
    //  to static factory methods, e.g., Monitor.newEnter(var).
    //  But for consistency, we also need to modify all other Stmt
    //  and replace their constructors by static factory methods.
    public enum Op {
        ENTER("enter"), EXIT("exit");

        private final String name;

        Op(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Op op;

    /**
     * Reference of the object to be locked/unlocked.
     */
    private final Var objectRef;

    public Monitor(Op op, Var objectRef) {
        this.op = op;
        this.objectRef = objectRef;
    }

    public boolean isEnter() {
        return op == Op.ENTER;
    }

    public boolean isExit() {
        return op == Op.EXIT;
    }

    public Var getObjectRef() {
        return objectRef;
    }

    @Override
    public List<Exp> getUses() {
        return List.of(objectRef);
    }

    @Override
    public boolean canFallThrough() {
        return true;
    }

    @Override
    public void accept(StmtVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(StmtRVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "monitor" + op + " " + objectRef;
    }
}
