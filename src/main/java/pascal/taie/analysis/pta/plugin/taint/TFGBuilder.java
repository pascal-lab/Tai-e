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

package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.flowgraph.ArrayIndexNode;
import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.InstanceFieldNode;
import pascal.taie.analysis.graph.flowgraph.InstanceNode;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.ObjectFlowGraph;
import pascal.taie.analysis.graph.flowgraph.VarNode;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Taint flow graph builder.
 */
class TFGBuilder {

    private static final Logger logger = LogManager.getLogger(TFGBuilder.class);

    private final PointerAnalysisResult pta;

    private final ObjectFlowGraph ofg;

    private final Set<TaintFlow> taintFlows;

    private final TaintManager taintManager;

    /**
     * Whether only track taint flow in application code.
     */
    private final boolean onlyApp = true;

    /**
     * Whether only track taint flow that reaches any sink.
     */
    private final boolean onlyReachSink = true;

    /**
     * Map from a node to set of taint objects pointed to by the node.
     */
    private Map<Node, Set<Obj>> node2TaintSet;

    TFGBuilder(PointerAnalysisResult pta,
               Set<TaintFlow> taintFlows,
               TaintManager taintManager) {
        this.pta = pta;
        this.ofg = pta.getObjectFlowGraph();
        this.taintFlows = taintFlows;
        this.taintManager = taintManager;
    }

    /**
     * Builds a complete taint flow graph.
     */
    private TaintFlowGraph buildComplete() {
        Set<Node> sourceNodes = collectSourceNodes();
        Set<Node> sinkNodes = collectSinkNode();
        // builds taint flow graph
        node2TaintSet = Maps.newMap();
        TaintFlowGraph tfg = new TaintFlowGraph(sourceNodes, sinkNodes);
        Set<Node> visitedNodes = Sets.newSet();
        Deque<Node> workList = new ArrayDeque<>(sourceNodes);
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            if (visitedNodes.add(node)) {
                getOutEdges(node).forEach(edge -> {
                    Node target = edge.target();
                    if (!onlyApp || isApp(target)) {
                        tfg.addEdge(edge);
                        if (!visitedNodes.contains(target)) {
                            workList.add(target);
                        }
                    }
                });
            }
        }
        node2TaintSet = null;
        return tfg;
    }

    private Set<Node> collectSourceNodes() {
        Set<Node> sourceNodes = Sets.newLinkedSet();
        for (Obj taintObj : taintManager.getTaintObjs()) {
            SourcePoint p = taintManager.getSourcePoint(taintObj);
            if (p instanceof CallSourcePoint csp) {
                IndexRef indexRef = csp.indexRef();
                Var var = InvokeUtils.getVar(csp.sourceCall(), indexRef.index());
                sourceNodes.addAll(getNodes(var, indexRef));
            } else if (p instanceof ParamSourcePoint psp) {
                IndexRef indexRef = psp.indexRef();
                Var var = psp.sourceMethod().getIR().getParam(indexRef.index());
                sourceNodes.addAll(getNodes(var, indexRef));
            } else if (p instanceof FieldSourcePoint fsp) {
                Var lhs = fsp.loadField().getLValue();
                Node sourceNode = ofg.getVarNode(lhs);
                if (sourceNode != null) {
                    sourceNodes.add(sourceNode);
                }
            }
        }
        logger.info("Source nodes:");
        sourceNodes.forEach(logger::info);
        return sourceNodes;
    }

    private Set<Node> collectSinkNode() {
        Set<Node> sinkNodes = Sets.newLinkedSet();
        taintFlows.forEach(taintFlow -> {
            SinkPoint sinkPoint = taintFlow.sinkPoint();
            IndexRef indexRef = sinkPoint.indexRef();
            Var var = InvokeUtils.getVar(sinkPoint.sinkCall(), indexRef.index());
            sinkNodes.addAll(getNodes(var, indexRef));
        });
        logger.info("Sink nodes:");
        sinkNodes.forEach(logger::info);
        return sinkNodes;
    }

    private Set<Node> getNodes(Var baseVar, IndexRef indexRef) {
        return switch (indexRef.kind()) {
            case VAR -> {
                Node node = ofg.getVarNode(baseVar);
                yield node != null ? Set.of(node) : Set.of();
            }
            case ARRAY -> pta.getPointsToSet(baseVar)
                    .stream()
                    .map(ofg::getArrayIndexNode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            case FIELD -> {
                JField field = indexRef.field();
                yield pta.getPointsToSet(baseVar)
                        .stream()
                        .map(o -> ofg.getInstanceFieldNode(o, field))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        };
    }

    private List<FlowEdge> getOutEdges(Node source) {
        Set<Obj> sourceTaintSet = getTaintSet(source);
        List<FlowEdge> edges = new ArrayList<>();
        // collect OFG edges
        ofg.getOutEdgesOf(source).forEach(edge -> {
            switch (edge.kind()) {
                case LOCAL_ASSIGN, INSTANCE_STORE, ARRAY_STORE,
                        THIS_PASSING, PARAMETER_PASSING, OTHER -> {
                    edges.add(edge);
                }
                case CAST, INSTANCE_LOAD, ARRAY_LOAD, RETURN -> {
                    // check whether target node also contains the same
                    // taint objects as source node to filter spurious edges
                    Set<Obj> targetTaintSet = getTaintSet(edge.target());
                    if (!Collections.disjoint(sourceTaintSet, targetTaintSet)) {
                        edges.add(edge);
                    }
                }
            }
        });
        return edges;
    }

    private Set<Obj> getTaintSet(Node node) {
        Set<Obj> taintSet = node2TaintSet.get(node);
        if (taintSet == null) {
            taintSet = getPointsToSet(node)
                    .stream()
                    .filter(taintManager::isTaint)
                    .collect(Sets::newHybridSet, Set::add, Set::addAll);
            if (taintSet.isEmpty()) {
                taintSet = Set.of();
            }
            node2TaintSet.put(node, taintSet);
        }
        return taintSet;
    }

    private Set<Obj> getPointsToSet(Node node) {
        if (node instanceof VarNode varNode) {
            return pta.getPointsToSet(varNode.getVar());
        } else if (node instanceof InstanceFieldNode ifNode) {
            return pta.getPointsToSet(ifNode.getBase(), ifNode.getField());
        } else if (node instanceof ArrayIndexNode aiNode) {
            return pta.getPointsToSet(aiNode.getBase());
        } else {
            return Set.of();
        }
    }

    TaintFlowGraph build() {
        TaintFlowGraph complete = buildComplete();
        Set<Node> sourceNodes = complete.getSourceNodes();
        Set<Node> sinkNodes = complete.getSinkNodes();
        TaintFlowGraph tfg = new TaintFlowGraph(sourceNodes, sinkNodes);
        Set<Node> nodesReachSink = null;
        if (onlyReachSink) {
            nodesReachSink = Sets.newHybridSet();
            Reachability<Node> reachability = new Reachability<>(complete);
            for (Node sink : sinkNodes) {
                nodesReachSink.addAll(reachability.nodesCanReach(sink));
            }
        }
        Set<Node> visitedNodes = Sets.newSet();
        Deque<Node> workList = new ArrayDeque<>(complete.getSourceNodes());
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            if (visitedNodes.add(node)) {
                for (FlowEdge edge : complete.getOutEdgesOf(node)) {
                    Node target = edge.target();
                    if (!onlyReachSink || nodesReachSink.contains(target)) {
                        tfg.addEdge(edge);
                        if (!visitedNodes.contains(target)) {
                            workList.add(target);
                        }
                    }
                }
            }
        }
        return tfg;
    }

    private static boolean isApp(Node node) {
        if (node instanceof VarNode varNode) {
            return varNode.getVar().getMethod().isApplication();
        } else if (node instanceof InstanceNode iNode) {
            return iNode.getBase().getContainerMethod()
                    .stream()
                    .anyMatch(JMethod::isApplication);
        } else {
            return false;
        }
    }
}
