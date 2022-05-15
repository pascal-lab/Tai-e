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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

public interface Solver {

    AnalysisOptions getOptions();

    ClassHierarchy getHierarchy();

    TypeSystem getTypeSystem();

    CSManager getCSManager();

    HeapModel getHeapModel();

    ContextSelector getContextSelector();

    CallGraph<CSCallSite, CSMethod> getCallGraph();

    /**
     * Returns the points-to set of given pointer. If the pointer has not
     * been associated with a points-to set, this method will create a
     * new set and associate it with the pointer.
     */
    PointsToSet getPointsToSetOf(Pointer pointer);

    /**
     * Creates a new empty points-to set.
     */
    PointsToSet makePointsToSet();

    void setPlugin(Plugin plugin);

    /**
     * Starts this solver.
     */
    void solve();

    // ---------- side-effect APIs (begin) ----------
    void addPointsTo(Pointer pointer, PointsToSet pts);

    default void addPointsTo(Pointer pointer, CSObj csObj) {
        PointsToSet pts = makePointsToSet();
        pts.addObject(csObj);
        addPointsTo(pointer, pts);
    }

    // convenient APIs for adding var-points-to relations

    /**
     * Adds a context-sensitive variable points-to relation.
     *
     * @param context     context of the method which contains the variable
     * @param var         the variable
     * @param heapContext heap context for the object
     * @param obj         the object to be added
     */
    void addVarPointsTo(Context context, Var var, Context heapContext, Obj obj);

    void addVarPointsTo(Context context, Var var, CSObj csObj);

    void addVarPointsTo(Context context, Var var, PointsToSet pts);

    /**
     * Adds a context-sensitive array index points-to relation.
     *
     * @param arrayContext heap context of the array object
     * @param array        the array object
     * @param heapContext  heap context for the element
     * @param obj          the element to be stored into the array
     */
    void addArrayPointsTo(Context arrayContext, Obj array, Context heapContext, Obj obj);

    /**
     * Adds static field points-to relations.
     *
     * @param field the static field
     * @param pts   the objects to be added to the points-to set of the field.
     */
    void addStaticFieldPointsTo(JField field, PointsToSet pts);

    /**
     * Adds an edge "source -> target" to the PFG.
     */
    default void addPFGEdge(Pointer source, Pointer target, PointerFlowEdge.Kind kind) {
        addPFGEdge(source, target, null, kind);
    }

    /**
     * Adds an edge "source -> target" to the PFG.
     * If type is not null, then we need to filter out assignable objects
     * in source points-to set.
     */
    void addPFGEdge(Pointer source, Pointer target, Type type, PointerFlowEdge.Kind kind);

    /**
     * Adds a call edge.
     *
     * @param edge the added edge.
     */
    void addCallEdge(Edge<CSCallSite, CSMethod> edge);

    /**
     * Adds a context-sensitive method.
     *
     * @param csMethod the added context-sensitive method.
     */
    void addCSMethod(CSMethod csMethod);

    /**
     * Analyzes the initializer of given class.
     *
     * @param cls the class to be initialized.
     */
    void initializeClass(JClass cls);

    // ---------- side-effect APIs (end) ----------

    /**
     * @return pointer analysis result.
     */
    PointerAnalysisResult getResult();
}
