/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.java.classes;

import pascal.taie.java.World;
import pascal.taie.java.types.Type;
import pascal.taie.pta.ir.IR;
import soot.SootMethod;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JMethod extends ClassMember {

    private final List<Type> paramTypes;

    private final Type returnType;

    private final Subsignature subsignature;

    private final SootMethod sootMethod; // TODO: <-- get rid of this

    private IR ir;

    public JMethod(JClass declaringClass, String name, Set<Modifier> modifiers,
                   List<Type> paramTypes, Type returnType,
                   SootMethod sootMethod) {
        super(declaringClass, name, modifiers);
        this.paramTypes = Collections.unmodifiableList(paramTypes);
        this.returnType = returnType;
        this.signature = StringReps.getSignatureOf(this);
        this.subsignature = Subsignature.get(name, paramTypes, returnType);
        this.sootMethod = sootMethod;
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

    public Subsignature getSubsignature() {
        return subsignature;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public IR getIR() {
        assert !isAbstract();
        if (ir == null) {
            ir = World.getIRBuilder().build(this);
        }
        return ir;
    }

    /**
     * @return the {@link MethodRef} pointing to this method.
     */
    public MethodRef getRef() {
        return MethodRef.get(declaringClass, name,
                paramTypes, returnType);
    }
}
