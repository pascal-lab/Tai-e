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

package pascal.taie.language.classes;

import pascal.taie.World;
import pascal.taie.analysis.oldpta.ir.PTAIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.types.ClassType;
import pascal.taie.language.types.Type;

import java.util.List;
import java.util.Set;

import static pascal.taie.util.collection.CollectionUtils.freeze;

public class JMethod extends ClassMember {

    private final List<Type> paramTypes;

    private final Type returnType;

    private final List<ClassType> exceptions;

    private final Subsignature subsignature;

    /**
     * Source of the body (and/or other information) of this method.
     * IRBuilder can use this to build method IR.
     */
    private final Object methodSource;

    private PTAIR ptair;

    private IR ir;

    public JMethod(JClass declaringClass, String name, Set<Modifier> modifiers,
                   List<Type> paramTypes, Type returnType,
                   List<ClassType> exceptions, Object methodSource) {
        super(declaringClass, name, modifiers);
        this.paramTypes = freeze(paramTypes);
        this.returnType = returnType;
        this.exceptions = freeze(exceptions);
        this.signature = StringReps.getSignatureOf(this);
        this.subsignature = Subsignature.get(name, paramTypes, returnType);
        this.methodSource = methodSource;
    }

    public boolean isAbstract() {
        return Modifier.hasAbstract(modifiers);
    }

    public boolean isNative() {
        return Modifier.hasNative(modifiers);
    }

    public int getParamCount() {
        return paramTypes.size();
    }

    public Type getParamType(int i) {
        return paramTypes.get(i);
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<ClassType> getExceptions() {
        return exceptions;
    }

    public Subsignature getSubsignature() {
        return subsignature;
    }

    public Object getMethodSource() {
        return methodSource;
    }

    @Deprecated
    public PTAIR getPTAIR() {
        assert !isAbstract();
        if (ptair == null) {
            ptair = World.getIRBuilder().buildPTAIR(this);
        }
        return ptair;
    }

    public IR getIR() {
        assert !isAbstract();
        if (ir == null) {
            if (isNative()) {
                ir = World.getNativeModel().buildNativeIR(this);
            } else {
                ir = World.getIRBuilder().buildIR(this);
            }
        }
        return ir;
    }

    /**
     * @return the {@link MethodRef} pointing to this method.
     */
    public MethodRef getRef() {
        return MethodRef.get(declaringClass, name,
                paramTypes, returnType, isStatic());
    }
}
