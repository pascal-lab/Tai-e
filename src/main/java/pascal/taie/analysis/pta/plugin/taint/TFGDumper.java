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
import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.InstanceFieldNode;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.OtherFlowEdge;
import pascal.taie.analysis.graph.flowgraph.VarNode;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.DotAttributes;
import pascal.taie.util.graph.DotDumper;
import pascal.taie.util.graph.Edge;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

/**
 * Taint flow graph dumper.
 */
class TFGDumper extends DotDumper<Node> {

    private static final Logger logger = LogManager.getLogger(TFGDumper.class);

    private static final transient Function TO_NULL = __ -> null;

    private static final transient Function TO_QUOTED_STRING =
            o -> "\"" + o.toString().replace("\"", "\\\"") + "\"";

    private final Set<String> highlightNodes;

    private TaintFlowGraph tfg;

    TFGDumper() {
        this(null);
    }

    TFGDumper(@Nullable String highlightPath) {
        highlightNodes = Sets.newSet();
        try {
            if (highlightPath != null) {
                highlightNodes.addAll(Files.readAllLines(Path.of(highlightPath)));
            }
        } catch (IOException e) {
            logger.warn("Failed to read highlight nodes from {}",
                    highlightPath, e);
        }
    }

    void dump(TaintFlowGraph tfg, File output) {
        logger.info("Dumping {}", output.getAbsolutePath());
        setNodeAttributer(this::nodeAttributer);
        setEdgeAttributer(this::edgeAttributer);
        this.tfg = tfg;
        super.dump(tfg, output);
    }

    @Override
    protected void dumpOthers() {
        Set<Object> dumped = Sets.newSet();
        // dump all Source and Source->SourceNode
        for (var entry : tfg.getSourceNode2SourcePoint().entrySet()) {
            Node sourceNode = entry.getKey();
            SourcePoint sourcePoint = entry.getValue();
            String elem = sourcePoint.source().rawEntry() + "\\n" + sourcePoint;
            if (dumped.add(elem)) {
                dumpElement(elem, TO_QUOTED_STRING, TO_NULL,
                        __ -> DotAttributes.of(
                                "shape", "doubleoctagon",
                                "fillcolor", "gold",
                                "style", "filled"));
            }
            dumpElement(TO_QUOTED_STRING.apply(elem)
                            + " -> " + TO_QUOTED_STRING.apply(sourceNode),
                    Object::toString, TO_NULL, TO_NULL);
        }
        // dump all Sink and Sink->SinkNode
        for (var entry : tfg.getSinkNode2SinkPoint().entrySet()) {
            Node sinkNode = entry.getKey();
            SinkPoint sinkPoint = entry.getValue();
            String elem = sinkPoint.sink().rawEntry() + "\\n" + sinkPoint;
            if (dumped.add(elem)) {
                dumpElement(elem, TO_QUOTED_STRING, TO_NULL,
                        __ -> DotAttributes.of(
                                "shape", "doubleoctagon",
                                "fillcolor", "deepskyblue",
                                "style", "filled"));
            }
            dumpElement(TO_QUOTED_STRING.apply(sinkNode)
                            + " -> " + TO_QUOTED_STRING.apply(elem),
                    Object::toString, TO_NULL, TO_NULL);
        }
    }

    private DotAttributes nodeAttributer(Node node) {
        DotAttributes attrs;
        if (node instanceof VarNode) {
            attrs = DotAttributes.of("shape", "box",
                    "style", "filled", "fillcolor", "floralwhite");
        } else if (node instanceof InstanceFieldNode) {
            attrs = DotAttributes.of("shape", "box",
                    "style", "rounded", "style", "filled", "fillcolor", "aliceblue");
        } else { // ArrayIndexNode
            attrs = DotAttributes.of("style", "filled", "fillcolor", "khaki1");
        }
        if (highlightNodes.contains(node.toString())) {
            attrs = attrs.update("fillcolor", "green1");
        }
        return attrs;
    }

    private DotAttributes edgeAttributer(Edge<Node> edge) {
        FlowEdge flowEdge = (FlowEdge) edge;
        DotAttributes attrs = switch (flowEdge.kind()) {
            case LOCAL_ASSIGN, CAST -> DotAttributes.of();
            case THIS_PASSING, PARAMETER_PASSING -> DotAttributes.of("color", "blue");
            case RETURN -> DotAttributes.of("color", "blue", "style", "dashed");
            case INSTANCE_STORE, ARRAY_STORE -> DotAttributes.of("color", "red");
            case INSTANCE_LOAD, ARRAY_LOAD -> DotAttributes.of("color", "red", "style", "dashed");
            case OTHER -> {
                if (edge instanceof OtherFlowEdge fe
                        && fe.rawEdge() instanceof TaintTransferEdge e) {
                    yield DotAttributes.of("color", "green3", "style", "dashed",
                            "label", (String) TO_QUOTED_STRING.apply(
                                    e.getTransfer().rawEntry()));
                } else {
                    yield DotAttributes.of("color", "green3", "style", "dashed");
                }
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported edge kind: " + flowEdge.kind());
        };
        if (highlightNodes.contains(flowEdge.source().toString()) &&
                highlightNodes.contains(flowEdge.target().toString())) {
            return attrs.add("style", "bold");
        } else {
            return attrs;
        }
    }

}
