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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import java.util.Set;

import static pascal.taie.util.collection.SetUtils.newSet;

/**
 * Triggers the analysis of class initializers.
 * Well, the description of "when initialization occurs" of JLS (11 Ed., 12.4.1)
 * and JVM Spec. (11 Ed., 5.5) looks not very consistent.
 * TODO: handles class initialization triggered by reflection,
 *  MethodHandle, and superinterfaces (that declare default methods).
 */
public class ClassInitializer implements Plugin, StmtVisitor {

    private Solver solver;

    private CSManager csManager;

    private Context defContext;

    /**
     * Set of classes that have been initialized.
     */
    private final Set<JClass> initializedClasses = newSet();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        csManager = solver.getCSManager();
        defContext = solver.getContextSelector().getDefaultContext();
    }

    @Override
    public void onNewMethod(JMethod method) {
        if (method.isStatic()) {
            initializeClass(method.getDeclaringClass());
        }
        method.getIR().getStmts().forEach(s -> s.accept(this));
    }

    @Override
    public void visit(New stmt) {
        initializeClass(extractClass(stmt.getRValue().getType()));
    }

    @Override
    public void visit(AssignLiteral stmt) {
        Literal rvalue = stmt.getRValue();
        initializeClass(extractClass(rvalue.getType()));
        if (rvalue instanceof ClassLiteral) {
            initializeClass(extractClass(
                    ((ClassLiteral) rvalue).getTypeValue()));
        }
    }

    /**
     * Analyzes the initializer of given class.
     */
    private void initializeClass(JClass cls) {
        if (cls == null || initializedClasses.contains(cls)) {
            return;
        }
        // initialize super class
        JClass superclass = cls.getSuperClass();
        if (superclass != null) {
            initializeClass(superclass);
        }
        // TODO: initialize the superinterfaces which
        //  declare default methods
        JMethod clinit = cls.getClinit();
        if (clinit != null) {
            // processNewCSMethod() may trigger initialization of more
            // classes. So cls must be added before processNewCSMethod(),
            // otherwise, infinite recursion may occur.
            initializedClasses.add(cls);
            CSMethod csMethod = csManager.getCSMethod(defContext, clinit);
            solver.addCSMethod(csMethod);
        }
    }

    /**
     * Extracts the class to be initialized from given type.
     */
    private JClass extractClass(Type type) {
        if (type instanceof ClassType) {
            return ((ClassType) type).getJClass();
        } else if (type instanceof ArrayType) {
            return extractClass(((ArrayType) type).getBaseType());
        }
        // Some types do not contain class to be initialized,
        // e.g., int[], then return null for such cases.
        return null;
    }

    @Override
    public void visit(Invoke stmt) {
        if (!stmt.isDynamic()) {
            processMemberRef(stmt.getMethodRef());
        }
        // TODO: check if the declaring class of bootstrap method
        //  of invokedynamic instruction needs to be initialized
    }

    @Override
    public void visit(LoadField stmt) {
        processMemberRef(stmt.getFieldRef());
    }

    @Override
    public void visit(StoreField stmt) {
        processMemberRef(stmt.getFieldRef());
    }

    private void processMemberRef(MemberRef memberRef) {
        if (memberRef.isStatic()) {
            initializeClass(memberRef.resolve().getDeclaringClass());
        }
    }
}
