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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.DefaultCallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.graph.flowgraph.ObjectFlowGraph;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowGraph;
import pascal.taie.analysis.pta.core.solver.PropagateTypes;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.util.AbstractResultHolder;
import pascal.taie.util.Canonicalizer;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.HybridBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class PointerAnalysisResultImpl extends AbstractResultHolder
        implements PointerAnalysisResult {

    private static final Logger logger = LogManager.getLogger(PointerAnalysisResultImpl.class);

    private final PropagateTypes propTypes;

    private final CSManager csManager;

    /**
     * Points-to set of local variables.
     */
    private final Map<Var, Set<Obj>> varPointsTo = Maps.newConcurrentMap(4096);

    /**
     * Points-to sets of instance field expressions, e.g., v.f.
     */
    private final Map<Pair<Var, JField>, Set<Obj>> ifieldPointsTo = Maps.newConcurrentMap(1024);

    /**
     * Points-to set of static field expressions, e.g., T.f.
     */
    private final Map<JField, Set<Obj>> sfieldPointsTo = Maps.newConcurrentMap(512);

    /**
     * Points-to set of array expressions, e.g., a[i].
     */
    private final Map<Var, Set<Obj>> arrayPointsTo = Maps.newConcurrentMap(1024);

    /**
     * Set of all (reachable) objects in the program.
     */
    private final Set<Obj> objects;

    /**
     * Canonicalizes (context-insensitive) points-to set.
     */
    private final Canonicalizer<Set<Obj>> canonicalizer = new Canonicalizer<>();

    /**
     * Context-sensitive call graph.
     */
    private final CallGraph<CSCallSite, CSMethod> csCallGraph;

    /**
     * Obj indexer.
     */
    private final Indexer<Obj> objIndexer;

    /**
     * Call graph (context projected out).
     */
    private CallGraph<Invoke, JMethod> callGraph;

    private final PointerFlowGraph pfg;

    /**
     * Object flow graph (context projected out).
     */
    private ObjectFlowGraph ofg;

    public PointerAnalysisResultImpl(
            PropagateTypes propTypes, CSManager csManager,
            Indexer<Obj> objIndexer, CallGraph<CSCallSite, CSMethod> csCallGraph,
            PointerFlowGraph pfg) {
        this.propTypes = propTypes;
        this.csManager = csManager;
        this.objIndexer = objIndexer;
        this.csCallGraph = csCallGraph;
        this.pfg = pfg;
        this.objects = removeContexts(getCSObjects().stream());
    }

    @Override
    public Collection<CSVar> getCSVars() {
        return csManager.getCSVars();
    }

    @Override
    public Collection<Var> getVars() {
        return csManager.getVars();
    }

    @Override
    public Collection<InstanceField> getInstanceFields() {
        return csManager.getInstanceFields();
    }

    @Override
    public Collection<ArrayIndex> getArrayIndexes() {
        return csManager.getArrayIndexes();
    }

    @Override
    public Collection<StaticField> getStaticFields() {
        return csManager.getStaticFields();
    }

    @Override
    public Collection<CSObj> getCSObjects() {
        return csManager.getObjects();
    }

    @Override
    public Collection<Obj> getObjects() {
        return objects;
    }

    @Override
    public Indexer<Obj> getObjectIndexer() {
        return objIndexer;
    }

    @Override
    public Set<Obj> getPointsToSet(Var var) {
        if (!propTypes.isAllowed(var)) {
            return Set.of();
        }
        return varPointsTo.computeIfAbsent(var, v ->
                removeContexts(csManager.getCSVarsOf(var)
                        .stream()
                        .flatMap(Pointer::objects)));
    }

    @Override
    public Set<Obj> getPointsToSet(InstanceFieldAccess access) {
        if (!propTypes.isAllowed(access)) {
            return Set.of();
        }
        Var base = access.getBase();
        JField field = access.getFieldRef().resolveNullable();
        return field != null ? getPointsToSet(base, field) : Set.of();
    }

    @Override
    public Set<Obj> getPointsToSet(Var base, JField field) {
        if (!propTypes.isAllowed(field.getType())) {
            return Set.of();
        }
        if (field.isStatic()) {
            logger.warn("{} is not an instance field", field);
            return Set.of();
        }
        // TODO - properly handle non-exist base.field
        return ifieldPointsTo.computeIfAbsent(new Pair<>(base, field), p ->
                removeContexts(csManager.getCSVarsOf(base)
                        .stream()
                        .flatMap(Pointer::objects)
                        .map(o -> csManager.getInstanceField(o, field))
                        .flatMap(InstanceField::objects)));
    }

    @Override
    public Set<Obj> getPointsToSet(Obj base, JField field) {
        if (!propTypes.isAllowed(field.getType())) {
            return Set.of();
        }
        if (field.isStatic()) {
            logger.warn("{} is not an instance field", field);
            return Set.of();
        }
        // TODO - properly handle non-exist base.field
        return removeContexts(csManager.getCSObjsOf(base)
                .stream()
                .map(o -> csManager.getInstanceField(o, field))
                .flatMap(InstanceField::objects));
    }

    @Override
    public Set<Obj> getPointsToSet(StaticFieldAccess access) {
        if (!propTypes.isAllowed(access)) {
            return Set.of();
        }
        JField field = access.getFieldRef().resolveNullable();
        return field != null ? getPointsToSet(field) : Set.of();
    }

    @Override
    public Set<Obj> getPointsToSet(JField field) {
        if (!propTypes.isAllowed(field.getType())) {
            return Set.of();
        }
        if (!field.isStatic()) {
            logger.warn("{} is not a static field", field);
            return Set.of();
        }
        return sfieldPointsTo.computeIfAbsent(field, f ->
                removeContexts(csManager.getStaticField(field).objects()));
    }

    @Override
    public Set<Obj> getPointsToSet(ArrayAccess access) {
        if (!propTypes.isAllowed(access)) {
            return Set.of();
        }
        return getPointsToSet(access.getBase(), access.getIndex());
    }

    @Override
    public Set<Obj> getPointsToSet(Var base, Var index) {
        if (base.getType() instanceof ArrayType baseType) {
            if (!propTypes.isAllowed(baseType.elementType())) {
                return Set.of();
            }
        } else {
            logger.warn("{} is not an array", base);
            return Set.of();
        }
        return arrayPointsTo.computeIfAbsent(base, b ->
                removeContexts(csManager.getCSVarsOf(b)
                        .stream()
                        .flatMap(Pointer::objects)
                        .map(csManager::getArrayIndex)
                        .flatMap(ArrayIndex::objects)));
    }

    @Override
    public Set<Obj> getPointsToSet(Obj array) {
        if (array.getType() instanceof ArrayType baseType) {
            if (!propTypes.isAllowed(baseType.elementType())) {
                return Set.of();
            }
        } else {
            logger.warn("{} is not an array", array);
            return Set.of();
        }
        return removeContexts(csManager.getCSObjsOf(array)
                .stream()
                .map(csManager::getArrayIndex)
                .flatMap(ArrayIndex::objects));
    }

    @Override
    public boolean mayAlias(Var v1, Var v2) {
        Set<Obj> s1 = getPointsToSet(v1);
        Set<Obj> s2 = getPointsToSet(v2);
        return !Collections.disjoint(s1, s2);
    }

    @Override
    public boolean mayAlias(InstanceFieldAccess if1, InstanceFieldAccess if2) {
        return Objects.equals(
                if1.getFieldRef().resolveNullable(),
                if2.getFieldRef().resolveNullable())
                && mayAlias(if1.getBase(), if2.getBase());
    }

    @Override
    public boolean mayAlias(ArrayAccess a1, ArrayAccess a2) {
        return mayAlias(a1.getBase(), a2.getBase());
    }

    /**
     * Removes contexts of a context-sensitive points-to set and
     * returns a new resulting set.
     */
    private Set<Obj> removeContexts(Stream<CSObj> objects) {
        Set<Obj> set = new HybridBitSet<>(objIndexer, true);
        objects.map(CSObj::getObject).forEach(set::add);
        return canonicalizer.get(Collections.unmodifiableSet(set));
    }

    @Override
    public CallGraph<CSCallSite, CSMethod> getCSCallGraph() {
        return csCallGraph;
    }

    @Override
    public CallGraph<Invoke, JMethod> getCallGraph() {
        if (callGraph == null) {
            callGraph = removeContexts(csCallGraph);
        }
        return callGraph;
    }

    /**
     * Removes contexts in a context-sensitive call graph and
     * returns a new resulting call graph.
     */
    private static CallGraph<Invoke, JMethod> removeContexts(
            CallGraph<CSCallSite, CSMethod> csCallGraph) {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        csCallGraph.entryMethods()
                .map(CSMethod::getMethod)
                .forEach(callGraph::addEntryMethod);
        csCallGraph.reachableMethods()
                .map(CSMethod::getMethod)
                .forEach(callGraph::addReachableMethod);
        csCallGraph.edges()
                .map(CIEdge::new)
                .forEach(callGraph::addEdge);
        return callGraph;
    }

    /**
     * Represents context-insensitive call edges.
     */
    private static class CIEdge extends Edge<Invoke, JMethod> {

        private static final Canonicalizer<String> canonicalizer = new Canonicalizer<>();

        private final String info;

        /**
         * Removes contexts and keeps info of given context-sensitive edge.
         */
        private CIEdge(Edge<CSCallSite, CSMethod> edge) {
            super(edge.getKind(),
                    edge.getCallSite().getCallSite(),
                    edge.getCallee().getMethod());
            this.info = canonicalizer.get(edge.getInfo());
        }

        @Override
        public String getInfo() {
            return info;
        }
    }

    public ObjectFlowGraph getObjectFlowGraph() {
        if (ofg == null) {
            ofg = new ObjectFlowGraph(pfg, getCallGraph());
        }
        return ofg;
    }
}
