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
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
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
import pascal.taie.util.collection.TwoKeyMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pascal.taie.analysis.pta.toolkit.zipper.FGEdge.Kind.INSTANCE_LOAD;
import static pascal.taie.analysis.pta.toolkit.zipper.FGEdge.Kind.INSTANCE_STORE;
import static pascal.taie.analysis.pta.toolkit.zipper.FGEdge.Kind.INTERPROCEDURAL_ASSIGN;
import static pascal.taie.analysis.pta.toolkit.zipper.FGEdge.Kind.LOCAL_ASSIGN;

class ObjectFlowGraph implements Graph<FGNode>, Indexer<FGNode> {

    private static final Logger logger = LogManager.getLogger(ObjectFlowGraph.class);

    private int nodeCounter;

    private final List<FGNode> nodes = new ArrayList<>(4096);

    private final Map<Var, VarNode> var2Node = Maps.newMap(4096);

    private final TwoKeyMap<Obj, JField, InstanceFieldNode> field2Node = Maps.newTwoKeyMap();

    private final Map<Obj, ArrayIndexNode> array2Node = Maps.newMap(1024);

    private final MultiMap<FGNode, FGEdge> inEdges = Maps.newMultiMap(
            new IndexMap<>(this, 4096));

    private final MultiMap<FGNode, FGEdge> outEdges = Maps.newMultiMap(
            new IndexMap<>(this, 4096));

    ObjectFlowGraph(PointerAnalysisResult pta) {
        CallGraph<Invoke, JMethod> callGraph = pta.getCallGraph();
        EdgeBuilder edgeBuilder = new EdgeBuilder(pta);
        nodeCounter = 0;
        callGraph.forEach(method ->
                method.getIR().forEach(s -> s.accept(edgeBuilder)));
        // log statistics
        logger.info("{} nodes in OFG", nodes.size());
        logger.info("{} edges in OFG",
                nodes.stream().mapToInt(this::getOutDegreeOf).sum());
    }

    private void addEdge(FGEdge.Kind kind, FGNode source, FGNode target) {
        FGEdge edge = new FGEdge(kind, source, target);
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
                addEdge(LOCAL_ASSIGN, fromNode, toNode);
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
                addEdge(LOCAL_ASSIGN, fromNode, toNode);
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
                    addEdge(INSTANCE_LOAD, fromNode, toNode);
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
                    addEdge(INSTANCE_STORE, fromNode, toNode);
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
                    addEdge(INSTANCE_LOAD, fromNode, toNode);
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
                    addEdge(INSTANCE_STORE, fromNode, toNode);
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
                            addEdge(INTERPROCEDURAL_ASSIGN, argNode, paramNode);
                        }
                    }
                    // add return-value edges
                    if (lhsNode != null) {
                        ir.getReturnVars()
                                .stream()
                                .map(ObjectFlowGraph.this::getOrCreateVarNode)
                                .forEach(retNode ->
                                        addEdge(INTERPROCEDURAL_ASSIGN, retNode, lhsNode));
                    }
                    // add receiver-passing edge
                    if (invoke.getInvokeExp() instanceof InvokeInstanceExp invokeExp) {
                        VarNode baseNode = getOrCreateVarNode(invokeExp.getBase());
                        VarNode thisNode = getOrCreateVarNode(ir.getThis());
                        addEdge(INTERPROCEDURAL_ASSIGN, baseNode, thisNode);
                    }
                }
            });
            return null;
        }

        private static boolean isRelevant(AssignStmt<?, ?> stmt) {
            return stmt.getLValue().getType() instanceof ReferenceType;
        }
    }

    private VarNode getOrCreateVarNode(Var var) {
        return var2Node.computeIfAbsent(var, v -> {
            VarNode node = new VarNode(v, nodeCounter++);
            nodes.add(node);
            return node;
        });
    }

    private InstanceFieldNode getOrCreateInstanceFieldNode(Obj base, JField field) {
        return field2Node.computeIfAbsent(base, field, (o, f) -> {
            InstanceFieldNode node = new InstanceFieldNode(o, f, nodeCounter++);
            nodes.add(node);
            return node;
        });
    }

    private ArrayIndexNode getOrCreateArrayIndexNode(Obj array) {
        return array2Node.computeIfAbsent(array, a -> {
            ArrayIndexNode node = new ArrayIndexNode(a, nodeCounter++);
            nodes.add(node);
            return node;
        });
    }

    @Override
    public boolean hasNode(FGNode node) {
        return nodes.contains(node);
    }

    @Override
    public boolean hasEdge(FGNode source, FGNode target) {
        return getSuccsOf(source).contains(target);
    }

    @Override
    public Set<FGNode> getPredsOf(FGNode node) {
        return Views.toMappedSet(getInEdgesOf(node), FGEdge::source);
    }

    @Override
    public Set<FGEdge> getInEdgesOf(FGNode node) {
        return inEdges.get(node);
    }

    @Override
    public Set<FGNode> getSuccsOf(FGNode node) {
        return Views.toMappedSet(getOutEdgesOf(node), FGEdge::target);
    }

    @Override
    public Set<FGEdge> getOutEdgesOf(FGNode node) {
        return outEdges.get(node);
    }

    @Override
    public Set<FGNode> getNodes() {
        return Views.toMappedSet(nodes, node -> node,
                o -> o instanceof FGNode node && hasNode(node));
    }

    @Override
    public int getIndex(FGNode o) {
        return o.getIndex();
    }

    @Override
    public FGNode getObject(int index) {
        return nodes.get(index);
    }

    @Nullable
    VarNode getVarNode(Var var) {
        return var2Node.get(var);
    }
}
