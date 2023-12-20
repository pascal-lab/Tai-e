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

package pascal.taie.analysis.graph.flowgraph;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JField;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;
import pascal.taie.util.collection.Views;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class NodeManager implements Indexer<Node> {

    private int nodeCounter;

    private final List<Node> nodes = new ArrayList<>(4096);

    private final Map<Var, VarNode> var2Node = Maps.newMap(4096);

    private final TwoKeyMap<Obj, JField, InstanceFieldNode> iField2Node = Maps.newTwoKeyMap();

    private final Map<Obj, ArrayIndexNode> array2Node = Maps.newMap(1024);

    private final Map<JField, StaticFieldNode> sField2Node = Maps.newMap(1024);

    @Nullable
    public VarNode getVarNode(Var var) {
        return var2Node.get(var);
    }

    protected VarNode getOrCreateVarNode(Var var) {
        return var2Node.computeIfAbsent(var, v -> {
            VarNode node = new VarNode(v, nodeCounter++);
            nodes.add(node);
            return node;
        });
    }

    @Nullable
    public InstanceFieldNode getInstanceFieldNode(Obj base, JField field) {
        return iField2Node.get(base, field);
    }

    protected InstanceFieldNode getOrCreateInstanceFieldNode(Obj base, JField field) {
        return iField2Node.computeIfAbsent(base, field, (o, f) -> {
            InstanceFieldNode node = new InstanceFieldNode(o, f, nodeCounter++);
            nodes.add(node);
            return node;
        });
    }

    @Nullable
    public ArrayIndexNode getArrayIndexNode(Obj array) {
        return array2Node.get(array);
    }

    protected ArrayIndexNode getOrCreateArrayIndexNode(Obj array) {
        return array2Node.computeIfAbsent(array, a -> {
            ArrayIndexNode node = new ArrayIndexNode(a, nodeCounter++);
            nodes.add(node);
            return node;
        });
    }

    protected StaticFieldNode getOrCreateStaticFieldNode(JField field) {
        return sField2Node.computeIfAbsent(field, f -> {
            StaticFieldNode node = new StaticFieldNode(f, nodeCounter++);
            nodes.add(node);
            return node;
        });
    }

    public boolean hasNode(Node node) {
        return nodes.contains(node);
    }

    public Set<Node> getNodes() {
        return Views.toMappedSet(nodes, node -> node,
                o -> o instanceof Node node && hasNode(node));
    }

    @Override
    public int getIndex(Node node) {
        return node.getIndex();
    }

    @Override
    public Node getObject(int index) {
        return nodes.get(index);
    }
}
