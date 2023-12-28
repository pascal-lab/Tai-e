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
 * Builds a {@link ClassGSignature}.
 */
final class ClassGSignatureBuilder extends TypeParameterAwareGSignatureBuilder {

    private final boolean isInterface;

    private ClassGSignature gSig;

    private ClassTypeGSignature superClass;

    private final List<ClassTypeGSignature> interfaces = new ArrayList<>();

    public ClassGSignatureBuilder(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public ClassGSignature get() {
        endInterface();
        endSuperClass();
        if (gSig == null) {
            gSig = new ClassGSignature(isInterface, typeParams,
                    superClass, interfaces);
        }
        return gSig;
    }

    @Override
    public SignatureVisitor visitSuperclass() {
        endInterfaceBound();
        endClassBound();
        endTypeParam();
        stack.push(State.VISIT_SUPERCLASS);
        return newTypeGSignatureBuilder();
    }

    /**
     * The valid automaton states for ending a {@link #visitSuperclass()} method call are:
     * <ul>
     *     <li>{@link #visitInterface()}</li>
     *     <li>{@link #get()}</li>
     * </ul>
     * When the automaton state is in one of these above states,
     * it is time to collect the superclass.
     */
    private void endSuperClass() {
        if (stack.peek() == State.VISIT_SUPERCLASS) {
            stack.pop();
            superClass = getTypeGSignature();
        }
    }

    @Override
    public SignatureVisitor visitInterface() {
        endInterface();
        endSuperClass();
        stack.push(State.VISIT_INTERFACE);
        return newTypeGSignatureBuilder();
    }

    /**
     * The valid automaton states for ending a {@link #visitInterface()} method call are:
     * <ul>
     *     <li>{@link #visitInterface()}</li>
     *     <li>{@link #get()}</li>
     * </ul>
     * When the automaton state is in one of these above states,
     * it is time to collect the interface.
     */
    private void endInterface() {
        if (stack.peek() == State.VISIT_INTERFACE) {
            stack.pop();
            interfaces.add(getTypeGSignature());
        }
    }

}
