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

package pascal.taie.util.graph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Configurable dot dumper.
 *
 * @param <N> type of graph nodes
 */
public class DotDumper<N> {

    private static final Logger logger = LogManager.getLogger(DotDumper.class);

    private static final String INDENT = "  ";

    /**
     * The output stream for dumping dot graph.
     */
    private PrintStream out;

    /**
     * The function that converts a node to its string representation.
     */
    private Function<N, String> nodeToString = Objects::toString;

    /**
     * Global node attributes.
     */
    private Map<String, String> globalNodeAttrs = Map.of();

    /**
     * The labeler for nodes.
     */
    private Function<N, String> nodeLabeler = node -> null;

    /**
     * The node attributes.
     */
    private Function<N, Map<String, String>> nodeAttrs = node -> null;

    /**
     * The labeler for edges.
     */
    private Function<Edge<N>, String> edgeLabeler = edge -> null;

    /**
     * Global edge attributes.
     */
    private Map<String, String> globalEdgeAttrs = Map.of();

    /**
     * The function that maps an edge to its attributes.
     */
    private Function<Edge<N>, Map<String, String>> edgeAttrs = edge -> null;

    public DotDumper<N> setNodeToString(Function<N, String> nodeToString) {
        this.nodeToString = nodeToString;
        return this;
    }

    public DotDumper<N> setGlobalNodeAttributes(Map<String, String> attrs) {
        globalNodeAttrs = attrs;
        return this;
    }

    public DotDumper<N> setNodeLabeler(Function<N, String> nodeLabeler) {
        this.nodeLabeler = nodeLabeler;
        return this;
    }

    public DotDumper<N> setNodeAttributes(
            Function<N, Map<String, String>> nodeAttrs) {
        this.nodeAttrs = nodeAttrs;
        return this;
    }

    public DotDumper<N> setEdgeLabeler(Function<Edge<N>, String> edgeLabeler) {
        this.edgeLabeler = edgeLabeler;
        return this;
    }

    public DotDumper<N> setGlobalEdgeAttributes(Map<String, String> attrs) {
        globalEdgeAttrs = attrs;
        return this;
    }

    public DotDumper<N> setEdgeAttrs(Function<Edge<N>, Map<String, String>> edgeAttrs) {
        this.edgeAttrs = edgeAttrs;
        return this;
    }

    public void dump(Graph<N> graph, String filePath) {
        dump(graph, new File(filePath));
    }

    public void dump(Graph<N> graph, File outFile) {
        try (PrintStream out = new PrintStream(new FileOutputStream(outFile))) {
            this.out = out;
            // dump starts
            out.println("digraph G {");
            // dump global node attributes
            if (!globalNodeAttrs.isEmpty()) {
                out.printf("%snode [", INDENT);
                dumpAttributes(globalNodeAttrs);
                out.println("];");
            }
            // dump global edge attributes
            if (!globalEdgeAttrs.isEmpty()) {
                out.printf("%sedge [", INDENT);
                dumpAttributes(globalEdgeAttrs);
                out.println("];");
            }
            // dump nodes
            graph.forEach(this::dumpNode);
            // dump edges
            graph.forEach(n -> graph.getOutEdgesOf(n).forEach(this::dumpEdge));
            // dump ends
            out.println("}");
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump graph to {}, caused by {}",
                    outFile.getAbsolutePath(), e);
        }
    }

    private void dumpAttributes(Map<String, String> attrs) {
        attrs.forEach((key, value) -> out.printf("%s=%s,", key, value));
    }

    private void dumpNode(N node) {
        dumpElement(node, this::getNodeRep, nodeLabeler, nodeAttrs);
    }

    private String getNodeRep(N node) {
        return "\"" + nodeToString.apply(node) + "\"";
    }

    private void dumpEdge(Edge<N> edge) {
        dumpElement(edge, this::getEdgeRep, edgeLabeler, edgeAttrs);
    }

    private String getEdgeRep(Edge<N> edge) {
        return getNodeRep(edge.getSource()) + " -> " +
                getNodeRep(edge.getTarget());
    }

    /**
     * Dumps an element (either a node or an edge).
     *
     * @param elem     element to be dumped
     * @param getRep   function that returns string representation of {@code elem}
     * @param getLabel function that returns label of {@code elem}
     * @param getAttrs function that returns attributes of {@code elem}
     * @param <T>      type of the element
     */
    private <T> void dumpElement(T elem,
                                 Function<T, String> getRep,
                                 Function<T, String> getLabel,
                                 Function<T, Map<String, String>> getAttrs) {
        out.print(INDENT);
        out.print(getRep.apply(elem));
        String label = getLabel.apply(elem);
        Map<String, String> attrs = getAttrs.apply(elem);
        if (label != null || attrs != null) {
            out.print(" [");
            if (label != null) {
                out.printf("label=\"%s\",", label);
            }
            if (attrs != null) {
                dumpAttributes(attrs);
            }
            out.print(']');
        }
        out.println(';');
    }
}
