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
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeManager;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

enum ImplicitThrowAnalysis {

    INSTANCE;

    static ImplicitThrowAnalysis get() {
        return INSTANCE;
    }

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
    private final StmtRVisitor<Collection<ClassType>> implicitVisitor
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
        public Collection<ClassType> visit(Invoke stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Collection<ClassType> visit(Throw stmt) {
            return NULL_POINTER_EXCEPTION;
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

    ImplicitThrowAnalysis() {
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

    Collection<ClassType> mayThrowImplicitly(Stmt stmt) {
        return stmt.accept(implicitVisitor);
    }

}
