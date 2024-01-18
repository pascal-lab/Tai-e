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

package pascal.taie.analysis.pta.toolkit.zipper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.InstanceNode;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.ObjectFlowGraph;
import pascal.taie.analysis.graph.flowgraph.VarNode;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class PFGBuilder {

    private static final Logger logger = LogManager.getLogger(PFGBuilder.class);

    private final PointerAnalysisResultEx pta;

    private final ObjectFlowGraph ofg;

    private final ObjectAllocationGraph oag;

    private final PotentialContextElement pce;

    /**
     * The input type.
     */
    private final Type type;

    /**
     * Methods invoked on objects of the input type.
     */
    private final Set<JMethod> invokeMethods;

    /**
     * Stores wrapped and unwrapped flow edges.
     */
    private MultiMap<Node, FlowEdge> wuEdges;

    private Set<Node> visitedNodes;

    private Set<VarNode> inNodes;

    private Set<VarNode> outNodes;

    PFGBuilder(PointerAnalysisResultEx pta, ObjectFlowGraph ofg,
               ObjectAllocationGraph oag, PotentialContextElement pce,
               Type type) {
        this.pta = pta;
        this.ofg = ofg;
        this.oag = oag;
        this.pce = pce;
        this.type = type;
        this.invokeMethods = pta.getObjectsOf(type)
                .stream()
                .map(pta::getMethodsInvokedOn)
                .flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    PrecisionFlowGraph build() {
        inNodes = obtainInNodes();
        outNodes = obtainOutNodes();
        visitedNodes = new IndexerBitSet<>(ofg, true);
        wuEdges = Maps.newMultiMap();
        for (VarNode inNode : inNodes) {
            dfs(inNode);
        }
        return new PrecisionFlowGraph(type, ofg, visitedNodes, outNodes, wuEdges);
    }

    private Set<JMethod> obtainMethods() {
        return pta.getObjectsOf(type)
                .stream()
                .map(pta::getMethodsInvokedOn)
                .flatMap(Set::stream)
                .filter(Predicate.not(JMethod::isPrivate))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<VarNode> obtainInNodes() {
        return obtainMethods()
                .stream()
                .flatMap(method -> method.getIR().getParams().stream())
                .filter(param -> !pta.getBase().getPointsToSet(param).isEmpty())
                .map(ofg::getVarNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<VarNode> obtainOutNodes() {
        Set<JMethod> outMethods = Sets.newSet(obtainMethods());
        // OUT methods of inner classes and special access$ methods
        // are also considered as the OUT methods of current type
        pce.pceMethodsOf(type)
                .stream()
                .filter(m -> !m.isPrivate() && !m.isStatic())
                .filter(m -> isInnerClass(m.getDeclaringClass()))
                .forEach(outMethods::add);
        pce.pceMethodsOf(type)
                .stream()
                .filter(m -> !m.isPrivate() && m.isStatic())
                .filter(m -> m.getDeclaringClass().getType().equals(type)
                        && m.getName().startsWith("access$"))
                .forEach(outMethods::add);
        return outMethods.stream()
                .flatMap(method -> method.getIR().getReturnVars().stream())
                .filter(ret -> !pta.getBase().getPointsToSet(ret).isEmpty())
                .map(ofg::getVarNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isInnerClass(JClass jclass) {
        if (type instanceof ClassType classType) {
            JClass outer = classType.getJClass();
            do {
                JClass inner = jclass;
                while (inner != null && !inner.equals(outer)) {
                    if (Objects.equals(inner.getOuterClass(), outer)) {
                        return true;
                    }
                    inner = inner.getOuterClass();
                }
                outer = outer.getSuperClass();
            } while (outer != null);
        }
        return false;
    }

    private void dfs(Node startNode) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(startNode);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (visitedNodes.contains(node)) {
                continue;
            }
            logger.trace("dfs on {}", node);
            visitedNodes.add(node);
            // add unwrapped flow edges
            if (node instanceof VarNode varNode) {
                Var var = varNode.getVar();
                Set<Obj> varPts = pta.getBase().getPointsToSet(var);
                // Optimization: approximate unwrapped flows to make
                // Zipper and pointer analysis run faster
                getReturnToVariablesOf(var).forEach(toVar -> {
                    VarNode toNode = ofg.getVarNode(toVar);
                    if (toNode != null && outNodes.contains(toNode)) {
                        for (VarNode inNode : inNodes) {
                            Var inVar = inNode.getVar();
                            if (!Collections.disjoint(
                                    pta.getBase().getPointsToSet(inVar), varPts)) {
                                wuEdges.put(node, new UnwrappedFlowEdge(node, toNode));
                                break;
                            }
                        }
                    }
                });
            }
            List<FlowEdge> nextEdges = new ArrayList<>();
            for (FlowEdge edge : getOutEdgesOf(node)) {
                switch (edge.kind()) {
                    case LOCAL_ASSIGN, CAST -> {
                        nextEdges.add(edge);
                    }
                    case INSTANCE_LOAD, ARRAY_LOAD,
                            THIS_PASSING, PARAMETER_PASSING, RETURN -> {
                        // target node must be a VarNode
                        VarNode toNode = (VarNode) edge.target();
                        Var toVar = toNode.getVar();
                        // Optimization: filter out some potential spurious flows due to
                        // the imprecision of context-insensitive pre-analysis, which
                        // helps improve the performance of Zipper and pointer analysis.
                        if (pce.pceMethodsOf(type).contains(toVar.getMethod())) {
                            nextEdges.add(edge);
                        }
                    }
                    case INSTANCE_STORE, ARRAY_STORE -> {
                        InstanceNode toNode = (InstanceNode) edge.target();
                        Obj base = toNode.getBase();
                        if (base.getType().equals(type)) {
                            // add wrapped flow edges to this variable
                            invokeMethods.stream()
                                    .map(m -> m.getIR().getThis())
                                    .map(ofg::getVarNode)
                                    .filter(Objects::nonNull) // filter this variable of native methods
                                    .forEach(nextNode -> wuEdges.put(toNode,
                                            new WrappedFlowEdge(toNode, nextNode)));
                            nextEdges.add(edge);
                        } else if (oag.getAllocateesOf(type).contains(base)) {
                            // Optimization, similar as above.
                            VarNode assignedNode = getAssignedNode(base);
                            if (assignedNode != null) {
                                wuEdges.put(toNode,
                                        new WrappedFlowEdge(toNode, assignedNode));
                            }
                            nextEdges.add(edge);
                        }
                    }
                    case OTHER -> {
                        if (edge instanceof WrappedFlowEdge) {
                            // same as INSTANCE_STORE
                            // target node must be a VarNode
                            VarNode toNode = (VarNode) edge.target();
                            Var toVar = toNode.getVar();
                            // Optimization: filter out some potential spurious flows due to
                            // the imprecision of context-insensitive pre-analysis, which
                            // helps improve the performance of Zipper and pointer analysis.
                            if (pce.pceMethodsOf(type).contains(toVar.getMethod())) {
                                nextEdges.add(edge);
                            }
                        } else if (edge instanceof UnwrappedFlowEdge) {
                            // same as LOCAL_ASSIGN
                            nextEdges.add(edge);
                        }
                    }
                }
            }
            for (FlowEdge nextEdge : nextEdges) {
                stack.push(nextEdge.target());
            }
        }
    }

    public Set<FlowEdge> getOutEdgesOf(Node node) {
        Set<FlowEdge> outEdges = ofg.getOutEdgesOf(node);
        if (wuEdges.containsKey(node)) {
            outEdges = Sets.newSet(outEdges);
            outEdges.addAll(wuEdges.get(node));
        }
        return outEdges;
    }

    @Nullable
    private VarNode getAssignedNode(Obj obj) {
        if (obj.getAllocation() instanceof New newStmt) {
            Var lhs = newStmt.getLValue();
            return ofg.getVarNode(lhs);
        }
        return null;
    }

    private static List<Var> getReturnToVariablesOf(Var var) {
        return var.getInvokes()
                .stream()
                .map(Invoke::getLValue)
                .filter(Objects::nonNull)
                .toList();
    }
}
