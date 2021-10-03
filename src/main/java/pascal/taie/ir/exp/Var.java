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

package pascal.taie.ir.exp;

import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of method/constructor parameters, lambda parameters,
 * exception parameters, and local variables.
 */
public class Var implements LValue, RValue {

    /**
     * The method containing this Var.
     */
    private final JMethod method;

    /**
     * The name of this Var.
     */
    private final String name;

    /**
     * The type of this Var.
     */
    private final Type type;

    /**
     * If this variable is a temporary variable generated to hold a constant value,
     * then this field holds that constant value; otherwise, this field is null.
     */
    private final Literal constValue;

    /**
     * Relevant statements of this variable.
     */
    private RelevantStmts relevantStmts = RelevantStmts.EMPTY;

    public Var(JMethod method, String name, Type type) {
        this(method, name, type, null);
    }

    public Var(JMethod method, String name, Type type, Literal constValue) {
        this.method = method;
        this.name = name;
        this.type = type;
        this.constValue = constValue;
    }

    /**
     * @return the method containing this Var.
     */
    public JMethod getMethod() {
        return method;
    }

    /**
     * @return name of this Var.
     */
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    /**
     * @return true if this variable is a temporary variable for holding
     * constant value, otherwise false.
     */
    public boolean isTempConst() {
        return constValue != null;
    }

    /**
     * @return the constant value held by this temporary variable.
     * @throws AnalysisException if this variable is not temporary variable
     */
    public Literal getTempConstValue() {
        if (!isTempConst()) {
            throw new AnalysisException(this + " is not a temporary variable" +
                    " that holds const value");
        }
        return constValue;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return name;
    }

    public void addLoadField(LoadField loadField) {
        ensureRelevantStmts();
        relevantStmts.addLoadField(loadField);
    }

    public List<LoadField> getLoadFields() {
        return relevantStmts.getLoadFields();
    }

    public void addStoreField(StoreField storeField) {
        ensureRelevantStmts();
        relevantStmts.addStoreField(storeField);
    }

    public List<StoreField> getStoreFields() {
        return relevantStmts.getStoreFields();
    }

    public void addLoadArray(LoadArray loadArray) {
        ensureRelevantStmts();
        relevantStmts.addLoadArray(loadArray);
    }

    public List<LoadArray> getLoadArrays() {
        return relevantStmts.getLoadArrays();
    }

    public void addStoreArray(StoreArray storeArray) {
        ensureRelevantStmts();
        relevantStmts.addStoreArray(storeArray);
    }

    public List<StoreArray> getStoreArrays() {
        return relevantStmts.getStoreArrays();
    }

    public void addInvoke(Invoke invoke) {
        ensureRelevantStmts();
        relevantStmts.addInvoke(invoke);
    }

    public List<Invoke> getInvokes() {
        return relevantStmts.getInvokes();
    }

    /**
     * Ensure {@link #relevantStmts} points to an instance other than
     * {@link RelevantStmts#EMPTY}.
     */
    private void ensureRelevantStmts() {
        if (relevantStmts == RelevantStmts.EMPTY) {
            relevantStmts = new RelevantStmts();
        }
    }

    /**
     * Relevant statements of a variable, say v, which include:
     * load field: x = v.f;
     * store field: v.f = x;
     * load array: x = v[i];
     * store array: v[i] = x;
     * invocation: v.f();
     * We use a separate class to store these relevant statements
     * (instead of directly storing them in {@link Var}) for saving space.
     * Most variables do not have any relevant statements, so these variables
     * only need to hold one reference to the empty {@link RelevantStmts},
     * instead of several references to empty lists.
     */
    private static class RelevantStmts {

        private static final RelevantStmts EMPTY = new RelevantStmts();

        private static final int DEFAULT_CAPACITY = 4;

        // Contract: if the following fields are empty, they must point to
        // Collections.emptyList();
        private List<LoadField> loadFields = List.of();
        private List<StoreField> storeFields = List.of();
        private List<LoadArray> loadArrays = List.of();
        private List<StoreArray> storeArrays = List.of();
        private List<Invoke> invokes = List.of();

        private List<LoadField> getLoadFields() {
            return unmodifiable(loadFields);
        }

        private void addLoadField(LoadField loadField) {
            if (loadFields.isEmpty()) {
                loadFields = new ArrayList<>();
            }
            loadFields.add(loadField);
        }

        private List<StoreField> getStoreFields() {
            return unmodifiable(storeFields);
        }

        private void addStoreField(StoreField storeField) {
            if (storeFields.isEmpty()) {
                storeFields = new ArrayList<>(DEFAULT_CAPACITY);
            }
            storeFields.add(storeField);
        }

        private List<LoadArray> getLoadArrays() {
            return unmodifiable(loadArrays);
        }

        private void addLoadArray(LoadArray loadArray) {
            if (loadArrays.isEmpty()) {
                loadArrays = new ArrayList<>(DEFAULT_CAPACITY);
            }
            loadArrays.add(loadArray);
        }

        private List<StoreArray> getStoreArrays() {
            return unmodifiable(storeArrays);
        }

        private void addStoreArray(StoreArray storeArray) {
            if (storeArrays.isEmpty()) {
                storeArrays = new ArrayList<>(DEFAULT_CAPACITY);
            }
            storeArrays.add(storeArray);
        }

        private List<Invoke> getInvokes() {
            return unmodifiable(invokes);
        }

        private void addInvoke(Invoke invoke) {
            if (invokes.isEmpty()) {
                invokes = new ArrayList<>(DEFAULT_CAPACITY);
            }
            invokes.add(invoke);
        }

        private static <T> List<T> unmodifiable(List<T> list) {
            return list.isEmpty() ? list : Collections.unmodifiableList(list);
        }
    }
}
