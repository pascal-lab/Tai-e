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
import pascal.taie.pta.env.Environment;

import java.util.Collection;

/**
 * Manages the whole-program information of the program being analyzed.
 */
public class World {

    /**
     * We maintain an instance instead of using static fields to ease the reset
     * of the world.
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

    private Environment environment;

    private JMethod mainMethod;

    private Collection<JMethod> implicitEntries;

    public static Options getOptions() {
        return theWorld.options;
    }

    public void setOptions(Options options) {
        theWorld.options = options;
    }

    public static TypeManager getTypeManager() {
        return theWorld.typeManager;
    }

    public void setTypeManager(TypeManager typeManager) {
        theWorld.typeManager = typeManager;
    }

    public static ClassHierarchy getClassHierarchy() {
        return theWorld.classHierarchy;
    }

    public void setClassHierarchy(ClassHierarchy classHierarchy) {
        theWorld.classHierarchy = classHierarchy;
    }

    public static IRBuilder getIRBuilder() {
        return theWorld.irBuilder;
    }

    public void setIRBuilder(IRBuilder irBuilder) {
        theWorld.irBuilder = irBuilder;
    }

    public static Environment getEnvironment() {
        return theWorld.environment;
    }

    public void setEnvironment(Environment environment) {
        theWorld.environment = environment;
    }

    public static JMethod getMainMethod() {
        return theWorld.mainMethod;
    }

    public void setMainMethod(JMethod mainMethod) {
        theWorld.mainMethod = mainMethod;
    }

    public static Collection<JMethod> getImplicitEntries() {
        return theWorld.implicitEntries;
    }

    public void setImplicitEntries(Collection<JMethod> implicitEntries) {
        theWorld.implicitEntries = implicitEntries;
    }
}
