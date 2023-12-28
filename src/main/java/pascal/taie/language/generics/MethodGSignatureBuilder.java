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


package pascal.taie.language.generics;

import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link MethodGSignature}.
 */
final class MethodGSignatureBuilder extends TypeParameterAwareGSignatureBuilder {

    private MethodGSignature gSig;

    private final List<TypeGSignature> params = new ArrayList<>();

    private TypeGSignature returnType;

    private final List<TypeGSignature> exceptionTypes = new ArrayList<>();

    public MethodGSignature get() {
        endExceptionType();
        endReturnType();
        if (gSig == null) {
            gSig = new MethodGSignature(typeParams, params,
                    returnType, exceptionTypes);
        }
        return gSig;
    }

    @Override
    public SignatureVisitor visitParameterType() {
        endInterfaceBound();
        endClassBound();
        endTypeParam();
        endParameterType();
        stack.push(State.VISIT_PARAMETER_TYPE);
        return newTypeGSignatureBuilder();
    }

    /**
     * The valid automaton states for ending a {@link #visitParameterType()} method call are:
     * <ul>
     *     <li>{@link #visitParameterType()}</li>
     *     <li>{@link #visitReturnType()}</li>
     * </ul>
     * When the automaton state is in one of these above states,
     * it is time to collect the parameter type.
     */
    private void endParameterType() {
        if (stack.peek() == State.VISIT_PARAMETER_TYPE) {
            stack.pop();
            params.add(getTypeGSignature());
        }
    }

    @Override
    public SignatureVisitor visitReturnType() {
        endInterfaceBound();
        endClassBound();
        endTypeParam();
        endParameterType();
        stack.push(State.VISIT_RETURN_TYPE);
        return newTypeGSignatureBuilder();
    }

    /**
     * The valid automaton states for ending a {@link #visitReturnType()} method call are:
     * <ul>
     *     <li>{@link #visitExceptionType()}</li>
     *     <li>{@link #get()}</li>
     * </ul>
     * When the automaton state is in one of these above states,
     * it is time to collect the return type.
     */
    private void endReturnType() {
        if (stack.peek() == State.VISIT_RETURN_TYPE) {
            stack.pop();
            returnType = getTypeGSignature();
        }
    }

    @Override
    public SignatureVisitor visitExceptionType() {
        endReturnType();
        endExceptionType();
        stack.push(State.VISIT_EXCEPTION_TYPE);
        return newTypeGSignatureBuilder();
    }

    /**
     * The valid automaton states for ending a {@link #visitExceptionType()} method call are:
     * <ul>
     *     <li>{@link #visitExceptionType()}</li>
     *     <li>{@link #get()}</li>
     * </ul>
     * When the automaton state is in one of these above states,
     * it is time to collect the exception type.
     */
    private void endExceptionType() {
        if (stack.peek() == State.VISIT_EXCEPTION_TYPE) {
            stack.pop();
            exceptionTypes.add(getTypeGSignature());
        }
    }

}
