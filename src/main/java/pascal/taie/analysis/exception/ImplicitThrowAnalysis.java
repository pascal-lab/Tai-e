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
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeManager;

import java.util.Set;

enum ImplicitThrowAnalysis {

    INSTANCE;

    static ImplicitThrowAnalysis get() {
        return INSTANCE;
    }

    // Implicit exception groups
    private final Set<ClassType> ARITHMETIC_EXCEPTION;

    private final Set<ClassType> LOAD_ARRAY_EXCEPTIONS;

    private final Set<ClassType> STORE_ARRAY_EXCEPTIONS;

    private final Set<ClassType> INITIALIZER_ERROR;

    private final Set<ClassType> CLASS_CAST_EXCEPTION;

    private final Set<ClassType> NEW_ARRAY_EXCEPTIONS;

    private final Set<ClassType> NULL_POINTER_EXCEPTION;

    private final Set<ClassType> OUT_OF_MEMORY_ERROR;

    /**
     * Visitor for compute implicit exceptions that may be thrown by each Stmt.
     */
    private final StmtVisitor<Set<ClassType>> implicitVisitor
            = new StmtVisitor<>() {
        @Override
        public Set<ClassType> visit(New stmt) {
            return stmt.getRValue() instanceof NewInstance ?
                    OUT_OF_MEMORY_ERROR : NEW_ARRAY_EXCEPTIONS;
        }

        @Override
        public Set<ClassType> visit(LoadArray stmt) {
            return LOAD_ARRAY_EXCEPTIONS;
        }

        @Override
        public Set<ClassType> visit(StoreArray stmt) {
            return STORE_ARRAY_EXCEPTIONS;
        }

        @Override
        public Set<ClassType> visit(LoadField stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(StoreField stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(Binary stmt) {
            if (stmt.getRValue() instanceof ArithmeticExp) {
                ArithmeticExp.Op op = ((ArithmeticExp) stmt.getRValue())
                        .getOperator();
                if (op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM) {
                    return ARITHMETIC_EXCEPTION;
                }
            }
            return Set.of();
        }

        @Override
        public Set<ClassType> visit(Unary stmt) {
            return stmt.getRValue() instanceof ArrayLengthExp ?
                    NULL_POINTER_EXCEPTION : Set.of();
        }

        @Override
        public Set<ClassType> visit(Cast stmt) {
            return CLASS_CAST_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(Invoke stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(Throw stmt) {
            return NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(Monitor stmt) {
            return NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visitDefault(Stmt stmt) {
            return Set.of();
        }
    };

    ImplicitThrowAnalysis() {
        TypeManager tm = World.get().getTypeManager();
        ClassType arrayStoreException = tm.getClassType(ClassNames.ARRAY_STORE_EXCEPTION);
        ClassType indexOutOfBoundsException = tm.getClassType(ClassNames.INDEX_OUT_OF_BOUNDS_EXCEPTION);
        ClassType nullPointerException = tm.getClassType(ClassNames.NULL_POINTER_EXCEPTION);
        ClassType outOfMemoryError = tm.getClassType(ClassNames.OUT_OF_MEMORY_ERROR);

        ARITHMETIC_EXCEPTION = Set.of(
                tm.getClassType(ClassNames.ARITHMETIC_EXCEPTION));
        LOAD_ARRAY_EXCEPTIONS = Set.of(
                indexOutOfBoundsException,
                nullPointerException);
        STORE_ARRAY_EXCEPTIONS = Set.of(
                arrayStoreException,
                indexOutOfBoundsException,
                nullPointerException);
        INITIALIZER_ERROR = Set.of(
                tm.getClassType(ClassNames.EXCEPTION_IN_INITIALIZER_ERROR));
        CLASS_CAST_EXCEPTION = Set.of(
                tm.getClassType(ClassNames.CLASS_CAST_EXCEPTION));
        NEW_ARRAY_EXCEPTIONS = Set.of(
                outOfMemoryError,
                tm.getClassType(ClassNames.NEGATIVE_ARRAY_SIZE_EXCEPTION));
        NULL_POINTER_EXCEPTION = Set.of(nullPointerException);
        OUT_OF_MEMORY_ERROR = Set.of(outOfMemoryError);
    }

    Set<ClassType> mayThrowImplicitly(Stmt stmt) {
        return stmt.accept(implicitVisitor);
    }

}
