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
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtRVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.types.ClassType;
import pascal.taie.language.types.TypeManager;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Common functionality for {@link ThrowAnalysis} implementations.
 * TODO: the exceptions handling is incomplete yet.
 */
public abstract class AbstractThrowAnalysis implements ThrowAnalysis {

    /**
     * Whether include implicit exceptions in the results.
     */
    protected final boolean includeImplicit;

    // Implicit exception groups
    protected final Collection<ClassType> ARITHMETIC_EXCEPTION;

    protected final Collection<ClassType> LOAD_ARRAY_EXCEPTIONS;

    protected final Collection<ClassType> STORE_ARRAY_EXCEPTIONS;

    protected final Collection<ClassType> INITIALIZER_ERROR;

    protected final Collection<ClassType> CLASS_CAST_EXCEPTION;

    protected final Collection<ClassType> NEW_ARRAY_EXCEPTIONS;

    protected final Collection<ClassType> NULL_POINTER_EXCEPTION;

    protected final Collection<ClassType> OUT_OF_MEMORY_ERROR;

    /**
     * Visitor for compute implicit exceptions that may be thrown by each Stmt.
     */
    protected final StmtRVisitor<Collection<ClassType>> implicitVisitor
            = new StmtRVisitor<>() {
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
    };

    protected AbstractThrowAnalysis(boolean includeImplicit) {
        this.includeImplicit = includeImplicit;

        TypeManager tm = World.getTypeManager();
        ClassType arrayStoreException = tm.getClassType(StringReps.ARRAY_STORE_EXCEPTION);
        ClassType indexOutOfBoundsException = tm.getClassType(StringReps.INDEX_OUT_OF_BOUNDS_EXCEPTION);
        ClassType nullPointerException = tm.getClassType(StringReps.NULL_POINTER_EXCEPTION);
        ClassType outOfMemoryError = tm.getClassType(StringReps.OUT_OF_MEMORY_ERROR);

        ARITHMETIC_EXCEPTION = List.of(
                tm.getClassType(StringReps.ARITHMETIC_EXCEPTION));
        LOAD_ARRAY_EXCEPTIONS = List.of(
                indexOutOfBoundsException,
                nullPointerException);
        STORE_ARRAY_EXCEPTIONS = List.of(
                arrayStoreException,
                indexOutOfBoundsException,
                nullPointerException);
        INITIALIZER_ERROR = List.of(
                tm.getClassType(StringReps.EXCEPTION_IN_INITIALIZER_ERROR));
        CLASS_CAST_EXCEPTION = List.of(
                tm.getClassType(StringReps.CLASS_CAST_EXCEPTION));
        NEW_ARRAY_EXCEPTIONS = List.of(
                outOfMemoryError,
                tm.getClassType(StringReps.NEGATIVE_ARRAY_SIZE_EXCEPTION));
        NULL_POINTER_EXCEPTION = List.of(nullPointerException);
        OUT_OF_MEMORY_ERROR = List.of(outOfMemoryError);
    }

    @Override
    public Result analyze(IR ir) {
        Object info = preAnalysis(ir);
        DefaultThrowAnalysisResult result = new DefaultThrowAnalysisResult();
        ir.getStmts().forEach(stmt -> {
            Collection<ClassType> exceptions = mayThrow(stmt, info);
            if (!exceptions.isEmpty()) {
                result.add(stmt, exceptions);
            }
        });
        return result;
    }

    protected Collection<ClassType> getImplicitExceptions(Stmt stmt) {
        return stmt.accept(implicitVisitor);
    }

    /**
     * Allow the concrete throw analysis to perform a pre-analysis to obtain
     * useful information which may be used when analyzing each Stmt.
     */
    protected Object preAnalysis(IR ir) {
        return null;
    }

    protected abstract Collection<ClassType> mayThrow(Stmt stmt, Object info);
}
