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

import pascal.taie.analysis.graph.flowgraph.ArrayIndexNode;
import pascal.taie.analysis.graph.flowgraph.InstanceFieldNode;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.util.graph.DotDumper;
import pascal.taie.util.graph.Graph;

import java.io.File;
import java.util.Map;

/**
 * Dumper for object/precision flow graph.
 */
class FlowGraphDumper {

    private static final DotDumper<Node> dumper = new DotDumper<Node>()
            .setNodeAttributes(n -> {
                if (n instanceof InstanceFieldNode) {
                    return Map.of("shape", "box");
                } else if (n instanceof ArrayIndexNode) {
                    return Map.of("shape", "box",
                            "style", "filled", "color", "grey");
                } else { // VarNode
                    return Map.of();
                }
            })
            .setEdgeAttrs(e -> {
                Edge edge = (Edge) e;
                return switch (edge.kind()) {
                    case LOCAL_ASSIGN, CAST -> Map.of();
                    case THIS_PASSING, PARAMETER_PASSING, RETURN -> Map.of("color", "blue");
                    case INSTANCE_STORE, ARRAY_STORE -> Map.of("color", "red");
                    case INSTANCE_LOAD, ARRAY_LOAD -> Map.of("color", "red", "style", "dashed");
                    case OTHER -> e instanceof WrappedEdge ?
                            Map.of("color", "green3") :
                            Map.of("color", "green3", "style", "dashed");
                    default -> throw new IllegalArgumentException(
                            "Unsupported edge kind: " + edge.kind());
                };
            });

    static void dump(Graph<Node> graph, File file) {
        dumper.dump(graph, file);
    }
}
