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

package pascal.taie.java;

import pascal.taie.java.classes.FieldRef;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodRef;
import pascal.taie.java.classes.Subsignature;
import pascal.taie.java.natives.NativeModel;
import pascal.taie.pta.env.Environment;

import java.util.Collection;

/**
 * Manages the whole-program information of the program being analyzed.
 */
public class World {

    /**
     * To store whole-program information, we maintain an instance,
     * (i.e., theWorld, with its instance fields pointing to the information),
     * instead of using static fields, to ease the reset of ZA WARUDO.
     */
    private static World theWorld;

    public static void set(World world) {
        theWorld = world;
    }

    public static void reset() {
        Subsignature.reset();
        FieldRef.reset();
        MethodRef.reset();
        theWorld = null;
    }

    private Options options;

    private TypeManager typeManager;

    private ClassHierarchy classHierarchy;

    private IRBuilder irBuilder;

    private NativeModel nativeModel;

    /**
     * Will be deprecated after removing old PTA.
     */
    @Deprecated
    private Environment environment;

    private JMethod mainMethod;

    private Collection<JMethod> implicitEntries;

    public static Options getOptions() {
        return theWorld.options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public static TypeManager getTypeManager() {
        return theWorld.typeManager;
    }

    public void setTypeManager(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    public static ClassHierarchy getClassHierarchy() {
        return theWorld.classHierarchy;
    }

    public void setClassHierarchy(ClassHierarchy classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    public static IRBuilder getIRBuilder() {
        return theWorld.irBuilder;
    }

    public void setIRBuilder(IRBuilder irBuilder) {
        this.irBuilder = irBuilder;
    }

    public static NativeModel getNativeModel() {
        return theWorld.nativeModel;
    }

    public void setNativeModel(NativeModel nativeModel) {
        this.nativeModel = nativeModel;
    }

    @Deprecated
    public static Environment getEnvironment() {
        return theWorld.environment;
    }

    @Deprecated
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public static JMethod getMainMethod() {
        return theWorld.mainMethod;
    }

    public void setMainMethod(JMethod mainMethod) {
        this.mainMethod = mainMethod;
    }

    public static Collection<JMethod> getImplicitEntries() {
        return theWorld.implicitEntries;
    }

    public void setImplicitEntries(Collection<JMethod> implicitEntries) {
        this.implicitEntries = implicitEntries;
    }
}
