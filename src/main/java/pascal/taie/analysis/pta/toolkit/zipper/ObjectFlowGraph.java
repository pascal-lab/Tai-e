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
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.flowgraph.ArrayIndexNode;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.graph.flowgraph.InstanceFieldNode;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.NodeManager;
import pascal.taie.analysis.graph.flowgraph.VarNode;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.IndexMap;
import pascal.taie.util.collection.Lists;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.List;
import java.util.Set;

class ObjectFlowGraph extends NodeManager
        implements Graph<Node>, Indexer<Node> {

    private static final Logger logger = LogManager.getLogger(ObjectFlowGraph.class);

    private final MultiMap<Node, Edge> inEdges = Maps.newMultiMap(
            new IndexMap<>(this, 4096));

    private final MultiMap<Node, Edge> outEdges = Maps.newMultiMap(
            new IndexMap<>(this, 4096));

    ObjectFlowGraph(PointerAnalysisResult pta) {
        CallGraph<Invoke, JMethod> callGraph = pta.getCallGraph();
        EdgeBuilder edgeBuilder = new EdgeBuilder(pta);
        callGraph.forEach(method ->
                method.getIR().forEach(s -> s.accept(edgeBuilder)));
        // log statistics
        logger.info("{} nodes in OFG", getNodes().size());
        logger.info("{} edges in OFG",
                getNodes().stream().mapToInt(this::getOutDegreeOf).sum());
    }

    private void addEdge(FlowKind kind, Node source, Node target) {
        BasicEdge edge = new BasicEdge(kind, source, target);
        outEdges.put(source, edge);
        inEdges.put(target, edge);
    }

    private class EdgeBuilder implements StmtVisitor<Void> {

        private final PointerAnalysisResult pta;

        private EdgeBuilder(PointerAnalysisResult pta) {
            this.pta = pta;
        }

        @Override
        public Void visit(Copy copy) {
            if (isRelevant(copy)) {
                Var to = copy.getLValue();
                Var from = copy.getRValue();
                VarNode toNode = getOrCreateVarNode(to);
                VarNode fromNode = getOrCreateVarNode(from);
                addEdge(FlowKind.LOCAL_ASSIGN, fromNode, toNode);
            }
            return null;
        }

        @Override
        public Void visit(Cast cast) {
            if (isRelevant(cast)) {
                Var to = cast.getLValue();
                Var from = cast.getRValue().getValue();
                VarNode toNode = getOrCreateVarNode(to);
                VarNode fromNode = getOrCreateVarNode(from);
                addEdge(FlowKind.CAST, fromNode, toNode);
            }
            return null;
        }

        @Override
        public Void visit(LoadField load) {
            if (isRelevant(load) &&
                    load.getFieldAccess() instanceof InstanceFieldAccess access) {
                Var to = load.getLValue();
                VarNode toNode = getOrCreateVarNode(to);
                Var base = access.getBase();
                JField field = access.getFieldRef().resolve();
                pta.getPointsToSet(base).forEach(obj -> {
                    InstanceFieldNode fromNode = getOrCreateInstanceFieldNode(obj, field);
                    addEdge(FlowKind.INSTANCE_LOAD, fromNode, toNode);
                });
            }
            return null;
        }

        @Override
        public Void visit(StoreField store) {
            if (isRelevant(store) &&
                    store.getFieldAccess() instanceof InstanceFieldAccess access) {
                Var base = access.getBase();
                JField field = access.getFieldRef().resolve();
                Var from = store.getRValue();
                VarNode fromNode = getOrCreateVarNode(from);
                pta.getPointsToSet(base).forEach(obj -> {
                    InstanceFieldNode toNode = getOrCreateInstanceFieldNode(obj, field);
                    addEdge(FlowKind.INSTANCE_STORE, fromNode, toNode);
                });
            }
            return null;
        }

        @Override
        public Void visit(LoadArray load) {
            if (isRelevant(load)) {
                Var to = load.getLValue();
                VarNode toNode = getOrCreateVarNode(to);
                Var base = load.getRValue().getBase();
                pta.getPointsToSet(base).forEach(array -> {
                    ArrayIndexNode fromNode = getOrCreateArrayIndexNode(array);
                    addEdge(FlowKind.ARRAY_LOAD, fromNode, toNode);
                });
            }
            return null;
        }

        @Override
        public Void visit(StoreArray store) {
            if (isRelevant(store)) {
                Var base = store.getLValue().getBase();
                Var from = store.getRValue();
                VarNode fromNode = getOrCreateVarNode(from);
                pta.getPointsToSet(base).forEach(array -> {
                    ArrayIndexNode toNode = getOrCreateArrayIndexNode(array);
                    addEdge(FlowKind.ARRAY_STORE, fromNode, toNode);
                });
            }
            return null;
        }

        @Override
        public Void visit(Invoke invoke) {
            Var lhs = invoke.getLValue();
            VarNode lhsNode = lhs != null && lhs.getType() instanceof ReferenceType
                    ? getOrCreateVarNode(lhs) : null;
            List<VarNode> argNodes = Lists.map(invoke.getInvokeExp().getArgs(),
                    ObjectFlowGraph.this::getOrCreateVarNode);
            pta.getCallGraph().edgesOutOf(invoke).forEach(edge -> {
                if (edge.getKind() != CallKind.OTHER) {
                    IR ir = edge.getCallee().getIR();
                    // add argument-passing edges
                    for (int i = 0; i < ir.getParams().size(); ++i) {
                        Var param = ir.getParam(i);
                        if (param.getType() instanceof ReferenceType) {
                            VarNode paramNode = getOrCreateVarNode(param);
                            VarNode argNode = argNodes.get(i);
                            addEdge(FlowKind.PARAMETER_PASSING, argNode, paramNode);
                        }
                    }
                    // add return-value edges
                    if (lhsNode != null) {
                        ir.getReturnVars()
                                .stream()
                                .map(ObjectFlowGraph.this::getOrCreateVarNode)
                                .forEach(retNode ->
                                        addEdge(FlowKind.RETURN, retNode, lhsNode));
                    }
                    // add receiver-passing edge
                    if (invoke.getInvokeExp() instanceof InvokeInstanceExp invokeExp) {
                        VarNode baseNode = getOrCreateVarNode(invokeExp.getBase());
                        VarNode thisNode = getOrCreateVarNode(ir.getThis());
                        addEdge(FlowKind.THIS_PASSING, baseNode, thisNode);
                    }
                }
            });
            return null;
        }

        private static boolean isRelevant(AssignStmt<?, ?> stmt) {
            return stmt.getLValue().getType() instanceof ReferenceType;
        }
    }

    @Override
    public boolean hasEdge(Node source, Node target) {
        return getSuccsOf(source).contains(target);
    }

    @Override
    public Set<Node> getPredsOf(Node node) {
        return Views.toMappedSet(getInEdgesOf(node), Edge::getSource);
    }

    @Override
    public Set<Edge> getInEdgesOf(Node node) {
        return inEdges.get(node);
    }

    @Override
    public Set<Node> getSuccsOf(Node node) {
        return Views.toMappedSet(getOutEdgesOf(node), Edge::getTarget);
    }

    @Override
    public Set<Edge> getOutEdgesOf(Node node) {
        return outEdges.get(node);
    }
}
