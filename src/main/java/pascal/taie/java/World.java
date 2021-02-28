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

import pascal.taie.java.classes.JMethod;
import pascal.taie.pta.env.Environment;

import java.util.Collection;

/**
 * Manages the whole-program information of the program being analyzed.
 */
public class World {

    private static World theWorld;

    public static World get() {
        return theWorld;
    }

    public static void set(World world) {
        theWorld = world;
    }

    private Options options;

    private TypeManager typeManager;

    private ClassHierarchy classHierarchy;

    private IRBuilder irBuilder;

    private JMethod mainMethod;

    private Collection<JMethod> implicitEntries;

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public TypeManager getTypeManager() {
        return typeManager;
    }

    public void setTypeManager(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    public ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }

    public void setClassHierarchy(ClassHierarchy classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    public IRBuilder getIRBuilder() {
        return irBuilder;
    }

    public void setIRBuilder(IRBuilder irBuilder) {
        this.irBuilder = irBuilder;
    }

    public JMethod getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(JMethod mainMethod) {
        this.mainMethod = mainMethod;
    }

    public Collection<JMethod> getImplicitEntries() {
        return implicitEntries;
    }

    public void setImplicitEntries(Collection<JMethod> implicitEntries) {
        this.implicitEntries = implicitEntries;
    }
}
