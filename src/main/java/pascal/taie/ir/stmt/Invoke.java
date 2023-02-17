/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Representation of invocation statement, e.g., r = o.m(...) or o.m(...).
 */
public class Invoke extends DefinitionStmt<Var, InvokeExp>
        implements Comparable<Invoke> {

    /**
     * The variable receiving the result of the invocation. This field
     * is null if no variable receives the invocation result, e.g., o.m(...).
     */
    @Nullable
    private final Var result;

    /**
     * The invocation expression.
     */
    private final InvokeExp invokeExp;

    /**
     * The method containing this statement.
     */
    private final JMethod container;

    public Invoke(JMethod method, InvokeExp invokeExp, @Nullable Var result) {
        this.invokeExp = invokeExp;
        this.result = result;
        if (invokeExp instanceof InvokeInstanceExp) {
            Var base = ((InvokeInstanceExp) invokeExp).getBase();
            base.addInvoke(this);
        }
        this.container = method;
    }

    public Invoke(JMethod method, InvokeExp invokeExp) {
        this(method, invokeExp, null);
    }

    @Override
    @Nullable
    public Var getLValue() {
        return result;
    }

    @Nullable
    public Var getResult() {
        return result;
    }

    @Override
    public InvokeExp getRValue() {
        return invokeExp;
    }

    /**
     * @return the invocation expression of this invoke.
     * @see InvokeExp
     */
    public InvokeExp getInvokeExp() {
        return invokeExp;
    }

    /**
     * @return the method reference of this invoke.
     * @see MethodRef
     */
    public MethodRef getMethodRef() {
        return invokeExp.getMethodRef();
    }

    public boolean isVirtual() {
        return invokeExp instanceof InvokeVirtual;
    }

    public boolean isInterface() {
        return invokeExp instanceof InvokeInterface;
    }

    public boolean isSpecial() {
        return invokeExp instanceof InvokeSpecial;
    }

    public boolean isStatic() {
        return invokeExp instanceof InvokeStatic;
    }

    public boolean isDynamic() {
        return invokeExp instanceof InvokeDynamic;
    }

    public JMethod getContainer() {
        return container;
    }

    @Override
    public Optional<LValue> getDef() {
        return Optional.ofNullable(result);
    }

    @Override
    public List<RValue> getUses() {
        return CollectionUtils.append(invokeExp.getUses(), invokeExp);
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }

    private static final Comparator<Invoke> COMPARATOR =
            Comparator.comparing((Invoke invoke) ->
                            invoke.getContainer().getDeclaringClass().toString())
                    .thenComparingInt(Stmt::getLineNumber)
                    .thenComparing(invoke -> invoke.getContainer().toString())
                    .thenComparingInt(Stmt::getIndex);

    @Override
    public int compareTo(@Nonnull Invoke other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        String ret = result == null ? "" : result + " = ";
        return String.format("%s[%d@L%d] %s%s",
                container, getIndex(), getLineNumber(), ret, invokeExp);
    }
}
