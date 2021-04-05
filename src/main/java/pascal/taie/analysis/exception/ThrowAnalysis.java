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

package pascal.taie.analysis.exception;

import pascal.taie.World;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtRVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.types.ClassType;
import pascal.taie.language.types.TypeManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static pascal.taie.util.collection.CollectionUtils.freeze;
import static pascal.taie.util.collection.CollectionUtils.newHybridSet;

/**
 * Intra-procedural throw analysis for computing the exceptions that
 * may be thrown by each Stmt.
 * TODO: the exceptions handling is incomplete yet.
 */
public class ThrowAnalysis {

    /**
     * Whether include implicit exceptions in the results.
     */
    private final boolean includeImplicit;
    
    // Implicit exception groups
    private final Collection<ClassType> ARITHMETIC_EXCEPTION;

    private final Collection<ClassType> LOAD_ARRAY_EXCEPTIONS;

    private final Collection<ClassType> STORE_ARRAY_EXCEPTIONS;

    private final Collection<ClassType> INITIALIZER_ERROR;

    private final Collection<ClassType> CLASS_CAST_EXCEPTION;

    private final Collection<ClassType> NEW_ARRAY_EXCEPTIONS;

    private final Collection<ClassType> NULL_POINTER_EXCEPTION;

    private final Collection<ClassType> OUT_OF_MEMORY_ERROR;

    /**
     * Visitor for compute implicit exceptions that may be thrown by each Stmt.
     */
    private final ImplicitVisitor visitor = new ImplicitVisitor();
    
    public ThrowAnalysis(boolean includeImplicit) {
        this.includeImplicit = includeImplicit;

        TypeManager tm = World.getTypeManager();
        ClassType arrayStoreException = tm.getClassType(StringReps.ARRAY_STORE_EXCEPTION);
        ClassType indexOutOfBoundsException = tm.getClassType(StringReps.INDEX_OUT_OF_BOUNDS_EXCEPTION);
        ClassType nullPointerException = tm.getClassType(StringReps.NULL_POINTER_EXCEPTION);
        ClassType outOfMemoryError = tm.getClassType(StringReps.OUT_OF_MEMORY_ERROR);

        ARITHMETIC_EXCEPTION = singletonList(
                tm.getClassType(StringReps.ARITHMETIC_EXCEPTION));
        LOAD_ARRAY_EXCEPTIONS = freeze(Arrays.asList(
                indexOutOfBoundsException,
                nullPointerException));
        STORE_ARRAY_EXCEPTIONS = freeze(Arrays.asList(
                arrayStoreException,
                indexOutOfBoundsException,
                nullPointerException));
        INITIALIZER_ERROR = singletonList(
                tm.getClassType(StringReps.EXCEPTION_IN_INITIALIZER_ERROR));
        CLASS_CAST_EXCEPTION = singletonList(
                tm.getClassType(StringReps.CLASS_CAST_EXCEPTION));
        NEW_ARRAY_EXCEPTIONS = freeze(Arrays.asList(
                outOfMemoryError,
                tm.getClassType(StringReps.NEGATIVE_ARRAY_SIZE_EXCEPTION)));
        NULL_POINTER_EXCEPTION = singletonList(nullPointerException);
        OUT_OF_MEMORY_ERROR = singletonList(outOfMemoryError);
    }

    public ThrowAnalysis() {
        this(false);
    }
    
    public Collection<ClassType> mayThrow(Stmt stmt) {
        if (stmt instanceof Throw) {
            return mayThrow((Throw) stmt);
        } else if (stmt instanceof Invoke) {
            return mayThrow((Invoke) stmt);
        } else if (includeImplicit) {
            return getImplicitExceptions(stmt);
        } else {
            return Collections.emptyList();
        }
    }

    private Collection<ClassType> mayThrow(Throw throwStmt) {
        Set<ClassType> result = newHybridSet();
        if (includeImplicit) {
            result.addAll(NULL_POINTER_EXCEPTION);
        }
        // add all subtypes of the type of thrown variable
        ClassType throwType = (ClassType) throwStmt.getExceptionRef().getType();
        World.getClassHierarchy()
                .getAllSubclassesOf(throwType.getJClass(), true)
                .stream()
                .map(JClass::getType)
                .forEach(result::add);
        return Collections.unmodifiableSet(result);
    }

    private Collection<ClassType> mayThrow(Invoke invoke) {
        Set<ClassType> result = newHybridSet();
        if (includeImplicit) {
            if (invoke.isStatic()) {
                result.addAll(INITIALIZER_ERROR);
            } else {
                result.addAll(NULL_POINTER_EXCEPTION);
            }
        }
        result.addAll(invoke.getMethodRef().resolve().getExceptions());
        return Collections.unmodifiableSet(result);
    }

    private Collection<ClassType> getImplicitExceptions(Stmt stmt) {
        return stmt.accept(visitor);
    }

    private class ImplicitVisitor implements StmtRVisitor<Collection<ClassType>> {

        @Override
        public Collection<ClassType> visit(New stmt) {
            return stmt.getRValue() instanceof NewInstance ?
                    OUT_OF_MEMORY_ERROR : NEW_ARRAY_EXCEPTIONS;
        }

        @Override
        public Collection<ClassType> visit(LoadArray stmt) {
            return LOAD_ARRAY_EXCEPTIONS;
        }

        @Override
        public Collection<ClassType> visit(StoreArray stmt) {
            return STORE_ARRAY_EXCEPTIONS;
        }

        @Override
        public Collection<ClassType> visit(LoadField stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Collection<ClassType> visit(StoreField stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Collection<ClassType> visit(Binary stmt) {
            if (stmt.getRValue() instanceof ArithmeticExp) {
                ArithmeticExp.Op op = ((ArithmeticExp) stmt.getRValue())
                        .getOperator();
                if (op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM) {
                    return ARITHMETIC_EXCEPTION;
                }
            }
            return emptyList();
        }

        @Override
        public Collection<ClassType> visit(Unary stmt) {
            return stmt.getRValue() instanceof ArrayLengthExp ?
                    NULL_POINTER_EXCEPTION : emptyList();
        }

        @Override
        public Collection<ClassType> visit(Cast stmt) {
            return CLASS_CAST_EXCEPTION;
        }

        @Override
        public Collection<ClassType> visit(Monitor stmt) {
            return NULL_POINTER_EXCEPTION;
        }

        @Override
        public Collection<ClassType> visitDefault(Stmt stmt) {
            return emptyList();
        }
    }
}
