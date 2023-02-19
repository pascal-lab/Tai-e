/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.flowgraph.ObjectFlowGraph;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Indexer;
import pascal.taie.util.ResultHolder;

import java.util.Collection;
import java.util.Set;

/**
 * Represents results of pointer analysis.
 * This class provides various API for querying points-to sets of
 * different kinds of pointer-accessing expressions. For the expressions
 * that are ignored by pointer analysis, an empty set is returned.
 */
public interface PointerAnalysisResult extends ResultHolder {

    /**
     * @return all reachable context-sensitive variables in the program.
     */
    Collection<CSVar> getCSVars();

    /**
     * @return all reachable variables in the program.
     */
    Collection<Var> getVars();

    /**
     * @return all reachable instance fields in the program.
     */
    Collection<InstanceField> getInstanceFields();

    /**
     * @return all reachable array indexes in the program.
     */
    Collection<ArrayIndex> getArrayIndexes();

    /**
     * @return all reachable static fields in the program.
     */
    Collection<StaticField> getStaticFields();

    /**
     * @return all reachable context-sensitive objects in the program.
     */
    Collection<CSObj> getCSObjects();

    /**
     * @return all reachable objects in the program.
     */
    Collection<Obj> getObjects();

    /**
     * @return indexer for Obj in the program.
     */
    Indexer<Obj> getObjectIndexer();

    /**
     * @return set of Obj pointed to by var.
     */
    Set<Obj> getPointsToSet(Var var);

    /**
     * @return set of Obj pointed to by field access.
     */
    default Set<Obj> getPointsToSet(FieldAccess access) {
        if (access instanceof InstanceFieldAccess ifaccess) {
            return getPointsToSet(ifaccess);
        } else {
            return getPointsToSet((StaticFieldAccess) access);
        }
    }

    /**
     * @return set of Obj pointed to by given instance field access, e.g., o.f.
     */
    Set<Obj> getPointsToSet(InstanceFieldAccess access);

    /**
     * @return set of Obj pointed to by base.field.
     */
    Set<Obj> getPointsToSet(Var base, JField field);

    /**
     * @return set of Obj pointed to by in given base.field.
     */
    Set<Obj> getPointsToSet(Obj base, JField field);

    /**
     * @return set of Obj pointed to by given static field access, e.g., T.f.
     */
    Set<Obj> getPointsToSet(StaticFieldAccess access);

    /**
     * @return points-to set of given field. The field is supposed to be static.
     */
    Set<Obj> getPointsToSet(JField field);

    /**
     * @return set of Obj pointed to by given array access, e.g., a[i].
     */
    Set<Obj> getPointsToSet(ArrayAccess access);

    /**
     * @return points-to set of given array index.
     * The base is supposed to be of array type; parameter index is unused.
     */
    Set<Obj> getPointsToSet(Var base, Var index);

    /**
     * @return set of Obj pointed to by given array.
     */
    Set<Obj> getPointsToSet(Obj array);

    /**
     * @return {@code true} if two variables may be aliases.
     */
    boolean mayAlias(Var v1, Var v2);

    /**
     * @return {@code true} if two instance field accesses may be aliases.
     */
    boolean mayAlias(InstanceFieldAccess if1, InstanceFieldAccess if2);

    /**
     * @return {@code true} if two array accesses may be aliases.
     */
    boolean mayAlias(ArrayAccess a1, ArrayAccess a2);

    /**
     * @return the resulting context-sensitive call graph.
     */
    CallGraph<CSCallSite, CSMethod> getCSCallGraph();

    /**
     * @return the resulting call graph (without contexts).
     */
    CallGraph<Invoke, JMethod> getCallGraph();

    /**
     * @return the resulting object flow graph.
     */
    ObjectFlowGraph getObjectFlowGraph();
}
