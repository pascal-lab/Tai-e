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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.util.graph.DotAttributes;
import pascal.taie.util.graph.DotDumper;
import pascal.taie.util.graph.Graph;

import java.io.File;

/**
 * Dumper for flow graph.
 */
public class FlowGraphDumper {

    private static final Logger logger = LogManager.getLogger(FlowGraphDumper.class);

    private static final DotDumper<Node> dumper = new DotDumper<Node>()
            .setNodeAttributer(n -> {
                if (n instanceof VarNode) {
                    return DotAttributes.of("shape", "box");
                } else if (n instanceof InstanceFieldNode) {
                    return DotAttributes.of("shape", "box",
                            "style", "rounded", "style", "filled",
                            "fillcolor", "aliceblue");
                } else { // ArrayIndexNode
                    return DotAttributes.of("style", "filled", "fillcolor", "khaki1");
                }
            })
            .setEdgeAttributer(e -> {
                FlowEdge edge = (FlowEdge) e;
                return switch (edge.kind()) {
                    case LOCAL_ASSIGN, CAST -> DotAttributes.of();
                    case THIS_PASSING, PARAMETER_PASSING -> DotAttributes.of("color", "blue");
                    case RETURN -> DotAttributes.of("color", "blue", "style", "dashed");
                    case INSTANCE_STORE, ARRAY_STORE -> DotAttributes.of("color", "red");
                    case INSTANCE_LOAD, ARRAY_LOAD -> DotAttributes.of("color", "red", "style", "dashed");
                    case OTHER -> DotAttributes.of("color", "green3", "style", "dashed");
                    default -> throw new IllegalArgumentException(
                            "Unsupported edge kind: " + edge.kind());
                };
            })
            .setEdgeLabeler(e -> {
                FlowEdge edge = (FlowEdge) e;
                return edge.kind() == FlowKind.OTHER ? e.getClass().getSimpleName() : "";
            });

    public static void dump(Graph<Node> graph, File output) {
        logger.info("Dumping {}", output.getAbsolutePath());
        dumper.dump(graph, output);
    }
}
