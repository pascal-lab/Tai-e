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

package pascal.taie.ir.exp;

import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.Type;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representation of invokedynamic instructions.
 * For more details about invokedynamic instructions, please refer to
 * https://docs.oracle.com/javase/7/docs/api/java/lang/invoke/package-summary.html
 */
public class InvokeDynamic extends InvokeExp {

    /**
     * Bootstrap method handle.
     * <p>
     * When created by Soot frontend, it will be {@code null}.
     * When create by new frontend it will be a {@link MethodHandle} instance.
     * <p>
     * Soot has already lost the information of
     * bootstrap method handle {@code methodHandle = kind + methodRef}
     * Soot lost the {@code kind} information.
     */
    @Nullable
    private final MethodHandle bootstrapMethodHandle;

    /**
     * Bootstrap method reference.
     * <p>
     * If {@link InvokeDynamic#bootstrapMethodHandle} is non-null,
     * it will be the same as {@code bootstrapMethodHandle.getMemberRef()}
     */
    private final MethodRef bootstrapMethodRef;

    private final String methodName;

    private final MethodType methodType;

    /**
     * Additional static arguments for bootstrap method.
     * As all these arguments are taken from the constant pool,
     * we store them as a list of Literals.
     */
    private final List<Literal> bootstrapArgs;

    public InvokeDynamic(MethodHandle bootstrapMethodhandle, MethodRef bootstrapMethodRef,
                         String methodName, MethodType methodType,
                         List<Literal> bootstrapArgs, List<Var> args) {
        super(null, args);
        this.bootstrapMethodHandle = bootstrapMethodhandle;
        this.bootstrapMethodRef = bootstrapMethodRef;
        this.methodName = methodName;
        this.methodType = methodType;
        this.bootstrapArgs = List.copyOf(bootstrapArgs);
    }

    public MethodHandle getBootstrapMethodHandle() {
        return bootstrapMethodHandle;
    }

    public MethodRef getBootstrapMethodRef() {
        return bootstrapMethodRef;
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public List<Literal> getBootstrapArgs() {
        return bootstrapArgs;
    }

    @Override
    public Type getType() {
        return methodType.getReturnType();
    }

    @Override
    public MethodRef getMethodRef() {
        throw new UnsupportedOperationException(
                "InvokeDynamic.getMethodRef() is unavailable");
    }

    @Override
    public String getInvokeString() {
        return "invokedynamic";
    }

    @Override
    public String toString() {
        return String.format("%s %s \"%s\" <%s>[%s]%s",
                getInvokeString(), bootstrapMethodRef,
                methodName, methodType,
                bootstrapArgs.stream()
                        .map(Literal::toString)
                        .collect(Collectors.joining(",")),
                getArgsString());
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
