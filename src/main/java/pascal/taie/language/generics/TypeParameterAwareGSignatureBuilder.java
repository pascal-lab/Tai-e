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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static pascal.taie.language.generics.TypeParameter.E;
import static pascal.taie.language.generics.TypeParameter.T;

/**
 * Collects a Signature that contains type parameters.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-TypeParameter">
 * JVM Spec. 4.7.9.1 TypeParameter</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-ClassSignature">
 * JVM Spec. 4.7.9.1 ClassSignature</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-MethodSignature">
 * JVM Spec. 4.7.9.1 MethodSignature</a>
 */
abstract class TypeParameterAwareGSignatureBuilder extends SignatureVisitor {

    protected final Deque<State> stack = new ArrayDeque<>();

    protected final List<TypeParameter> typeParams = new ArrayList<>();

    private TypeGSignatureBuilder typeGSigBuilder;

    // Type parameter related data

    private String typeName;

    private ReferenceTypeGSignature classBound;

    private final List<ReferenceTypeGSignature> interfaceBounds = new ArrayList<>();

    TypeParameterAwareGSignatureBuilder() {
        super(GSignatures.API);
    }

    // -------------------------------------------------------------------------------------
    // Class/Method signature common related methods
    // -------------------------------------------------------------------------------------

    @Override
    public void visitFormalTypeParameter(String name) {
        endInterfaceBound();
        endClassBound();
        endTypeParam();
        stack.push(State.VISIT_TYPE_PARAM);
        typeName = name;
    }

    /**
     * The valid automaton states for ending a {@link #visitFormalTypeParameter(String)} method call are:
     * <ul>
     *     <li>{@link #visitFormalTypeParameter(String)}</li>
     *     <li>{@link #visitSuperclass()}</li>
     *     <li>{@link #visitParameterType()}</li>
     *     <li>{@link #visitReturnType()}</li>
     * </ul>
     * When the automaton state is in one of these above states,
     * it is time to collect the current type parameter.
     */
    protected void endTypeParam() {
        if (stack.peek() == State.VISIT_TYPE_PARAM) {
            stack.pop();
            typeParams.add(buildTypeParameter());
        }
    }

    @Override
    public SignatureVisitor visitClassBound() {
        stack.push(State.VISIT_CLASS_BOUND);
        return newTypeGSignatureBuilder();
    }

    /**
     * The valid automaton states for ending a {@link #visitClassBound()} method call are:
     * <ul>
     *     <li>{@link #visitFormalTypeParameter(String)}</li>
     *     <li>{@link #visitInterfaceBound()}</li>
     *     <li>{@link #visitSuperclass()}</li>
     *     <li>{@link #visitParameterType()}</li>
     *     <li>{@link #visitReturnType()}</li>
     * </ul>
     * When the automaton state is in one of these above states,
     * it is time to collect the class bound of current type parameter.
     */
    protected void endClassBound() {
        if (stack.peek() == State.VISIT_CLASS_BOUND) {
            stack.pop();
            classBound = getTypeGSignature();
        }
    }

    @Override
    public SignatureVisitor visitInterfaceBound() {
        endInterfaceBound();
        endClassBound();
        stack.push(State.VISIT_INTERFACE_BOUND);
        return newTypeGSignatureBuilder();
    }

    /**
     * The valid automaton states for ending a {@link #visitInterfaceBound()} method call are:
     * <ul>
     *     <li>{@link #visitFormalTypeParameter(String)}</li>
     *     <li>{@link #visitInterfaceBound()}</li>
     *     <li>{@link #visitSuperclass()}</li>
     *     <li>{@link #visitParameterType()}</li>
     *     <li>{@link #visitReturnType()}</li>
     * </ul>
     * When the automaton state is in one of these above states,
     * it is time to collect the interface bound of current type parameter.
     */
    protected void endInterfaceBound() {
        if (stack.peek() == State.VISIT_INTERFACE_BOUND) {
            stack.pop();
            interfaceBounds.add(getTypeGSignature());
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------

    protected TypeGSignatureBuilder newTypeGSignatureBuilder() {
        assert typeGSigBuilder == null;
        typeGSigBuilder = new TypeGSignatureBuilder();
        return typeGSigBuilder;
    }

    @SuppressWarnings("unchecked")
    protected <T extends TypeGSignature> T getTypeGSignature() {
        assert typeGSigBuilder != null;
        T result = (T) typeGSigBuilder.get();
        typeGSigBuilder = null;
        return result;
    }

    private TypeParameter buildTypeParameter() {
        try {
            assert typeName != null;
            if (classBound == ClassTypeGSignature.JAVA_LANG_OBJECT
                    && interfaceBounds.isEmpty()
                    && (T.getTypeName().equals(typeName) || E.getTypeName().equals(typeName))) {
                return T.getTypeName().equals(typeName) ? T : E;
            }
            return new TypeParameter(typeName,
                    classBound, List.copyOf(interfaceBounds));
        } finally {
            typeName = null;
            classBound = null;
            interfaceBounds.clear();
        }
    }

    protected enum State {

        VISIT_TYPE_PARAM,

        VISIT_CLASS_BOUND,

        VISIT_INTERFACE_BOUND,

        VISIT_SUPERCLASS,

        VISIT_INTERFACE,

        VISIT_PARAMETER_TYPE,

        VISIT_RETURN_TYPE,

        VISIT_EXCEPTION_TYPE,
    }

}
