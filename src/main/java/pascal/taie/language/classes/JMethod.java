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
import pascal.taie.ir.IR;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import java.util.List;
import java.util.Set;

/**
 * Represents methods in the program. Each instance contains various
 * information of a method, including method name, signature, declaring class,
 * method body (IR), etc.
 */
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

    private IR ir;

    public JMethod(JClass declaringClass, String name, Set<Modifier> modifiers,
                   List<Type> paramTypes, Type returnType,
                   List<ClassType> exceptions, Object methodSource) {
        super(declaringClass, name, modifiers);
        this.paramTypes = List.copyOf(paramTypes);
        this.returnType = returnType;
        this.exceptions = List.copyOf(exceptions);
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

    public boolean isConstructor() {
        return name.equals(StringReps.INIT_NAME);
    }

    public boolean isStaticInitializer() {
        return name.equals(StringReps.CLINIT_NAME);
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
