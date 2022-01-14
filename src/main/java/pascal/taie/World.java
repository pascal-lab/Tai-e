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

package pascal.taie;

import pascal.taie.config.Options;
import pascal.taie.ir.IRBuilder;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.natives.NativeModel;
import pascal.taie.language.type.TypeManager;
import pascal.taie.util.AbstractResultHolder;
import pascal.taie.util.ResultHolder;

import java.util.Collection;

/**
 * Manages the whole-program information of the program being analyzed.
 */
public final class World {

    /**
     * To store whole-program information, we maintain an instance,
     * (i.e., theWorld, with its instance fields pointing to the information),
     * instead of using static fields, to ease the resetting of ZA WARUDO.
     */
    private static World theWorld;

    private Options options;

    private TypeManager typeManager;

    private ClassHierarchy classHierarchy;

    private IRBuilder irBuilder;

    private NativeModel nativeModel;

    private final ResultHolder resultHolder = new AbstractResultHolder() {
    };

    private JMethod mainMethod;

    private Collection<JMethod> implicitEntries;

    public static void set(World world) {
        theWorld = world;
    }

    /**
     * @return the current {@code World} instance.
     */
    public static World get() {
        return theWorld;
    }

    public static void reset() {
        Subsignature.reset();
        FieldRef.reset();
        MethodRef.reset();
        theWorld = null;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        if (this.options != null) {
            throw new IllegalStateException("Options already set");
        }
        this.options = options;
    }

    public TypeManager getTypeManager() {
        return typeManager;
    }

    public void setTypeManager(TypeManager typeManager) {
        if (this.typeManager != null) {
            throw new IllegalStateException("TypeManager already set");
        }
        this.typeManager = typeManager;
    }

    public ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }

    public void setClassHierarchy(ClassHierarchy classHierarchy) {
        if (this.classHierarchy != null) {
            throw new IllegalStateException("ClassHierarchy already set");
        }
        this.classHierarchy = classHierarchy;
    }

    public IRBuilder getIRBuilder() {
        return irBuilder;
    }

    public void setIRBuilder(IRBuilder irBuilder) {
        this.irBuilder = irBuilder;
    }

    public NativeModel getNativeModel() {
        return nativeModel;
    }

    public void setNativeModel(NativeModel nativeModel) {
        this.nativeModel = nativeModel;
    }

    public JMethod getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(JMethod mainMethod) {
        if (this.mainMethod != null) {
            throw new IllegalStateException("Main method already set");
        }
        this.mainMethod = mainMethod;
    }

    public Collection<JMethod> getImplicitEntries() {
        return implicitEntries;
    }

    public void setImplicitEntries(Collection<JMethod> implicitEntries) {
        this.implicitEntries = implicitEntries;
    }

    public static void storeResult(String id, Object result) {
        theWorld.resultHolder.storeResult(id, result);
    }

    public static <R> R getResult(String id) {
        return theWorld.resultHolder.getResult(id);
    }

    public static <R> R getResult(String id, R defaultResult) {
        return theWorld.resultHolder.getResult(id, defaultResult);
    }

    public static void clearResult(String id) {
        theWorld.resultHolder.clearResult(id);
    }
}
