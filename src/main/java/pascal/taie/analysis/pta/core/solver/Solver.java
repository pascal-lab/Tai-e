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
import pascal.taie.analysis.graph.flowgraph.FlowKind;
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
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

import java.util.Collection;
import java.util.function.Predicate;

public interface Solver {

    AnalysisOptions getOptions();

    ClassHierarchy getHierarchy();

    TypeSystem getTypeSystem();

    HeapModel getHeapModel();

    CSManager getCSManager();

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

    /**
     * Sets plugin to this solver.
     */
    void setPlugin(Plugin plugin);

    /**
     * Starts this solver.
     */
    void solve();

    // ---------- side-effect APIs (begin) ----------
    // These side-effect APIs could be used by Plugins to update
    // points-to information.

    // APIs for adding points-to relations
    void addPointsTo(Pointer pointer, PointsToSet pts);

    void addPointsTo(Pointer pointer, CSObj csObj);

    void addPointsTo(Pointer pointer, Context heapContext, Obj obj);

    /**
     * Convenient API to add points-to relation for object
     * with empty heap context.
     */
    default void addPointsTo(Pointer pointer, Obj obj) {
        addPointsTo(pointer, getContextSelector().getEmptyContext(), obj);
    }

    // convenient APIs for adding var-points-to relations
    void addVarPointsTo(Context context, Var var, PointsToSet pts);

    void addVarPointsTo(Context context, Var var, CSObj csObj);

    void addVarPointsTo(Context context, Var var, Context heapContext, Obj obj);

    /**
     * Convenient API to add var points-to relation for object
     * with empty heap context.
     */
    default void addVarPointsTo(Context context, Var var, Obj obj) {
        addVarPointsTo(context, var, getContextSelector().getEmptyContext(), obj);
    }

    /**
     * Adds an object filter to given pointer.
     * Note that the filter works only after it is added to the pointer,
     * and it cannot filter out the objects pointed to by the pointer
     * before it is added.
     */
    void addPointerFilter(Pointer pointer, Predicate<CSObj> filter);

    /**
     * Adds an edge "source -> target" to the PFG.
     */
    default void addPFGEdge(Pointer source, Pointer target, FlowKind kind) {
        addPFGEdge(new PointerFlowEdge(kind, source, target));
    }

    /**
     * Adds an edge "source -> target" to the PFG.
     * For the objects pointed to by "source", only the ones whose types
     * are subtypes of given type are propagated to "target".
     * @deprecated Use {@link #addPFGEdge(PointerFlowEdge, Type)} instead.
     */
    @Deprecated
    default void addPFGEdge(Pointer source, Pointer target, FlowKind kind, Type type) {
        addPFGEdge(new PointerFlowEdge(kind, source, target), type);
    }

    /**
     * Adds an edge "source -> target" (with edge transfer) to the PFG.
     * @deprecated Use {@link #addPFGEdge(PointerFlowEdge, Transfer)} instead.
     */
    @Deprecated
    default void addPFGEdge(Pointer source, Pointer target, FlowKind kind, Transfer transfer) {
        addPFGEdge(new PointerFlowEdge(kind, source, target), transfer);
    }

    /**
     * Adds a pointer flow edge to the PFG.
     */
    default void addPFGEdge(PointerFlowEdge edge) {
        addPFGEdge(edge, Identity.get());
    }

    /**
     * Adds a pointer flow edge (with type filer) to the PFG.
     * For the objects pointed to by {@code edge.source()},
     * only the ones whose types are subtypes of {@code type}
     * can be propagated to {@code edge.target()}.
     */
    default void addPFGEdge(PointerFlowEdge edge, Type type) {
        addPFGEdge(edge, new TypeFilter(type, this));
    }

    /**
     * Adds a pointer flow edge (with edge transfer) to the PFG.
     */
    void addPFGEdge(PointerFlowEdge edge, Transfer transfer);

    /**
     * Adds an entry point.
     * Notes that the method in entry point will be set as an entry in {@link CallGraph}
     */
    void addEntryPoint(EntryPoint entryPoint);

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
     * Adds stmts to the analyzed program. Solver will process given stmts.
     *
     * @param csMethod the container method of the stmts
     * @param stmts    the added stmts
     */
    void addStmts(CSMethod csMethod, Collection<Stmt> stmts);

    /**
     * If a plugin takes over the analysis of a method, and wants this solver
     * to ignore the method (for precision and/or efficiency reasons),
     * then it could call this API with the method.
     * After that, this solver will not process the method body.
     * <p>
     * Typically, this API should be called at the initial stage of
     * pointer analysis, i.e., in {@link Plugin#onStart()}.
     *
     * @param method the method to be ignored.
     */
    void addIgnoredMethod(JMethod method);

    /**
     * Analyzes the static initializer (i.e., &lt;clinit&gt;) of given class.
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
