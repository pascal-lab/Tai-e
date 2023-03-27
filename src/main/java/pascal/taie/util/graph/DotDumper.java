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
    private DotAttributes globalNodeAttrs = DotAttributes.of();

    /**
     * The labeler for nodes.
     */
    private Function<N, String> nodeLabeler = node -> null;

    /**
     * The node attributes.
     */
    private Function<N, DotAttributes> nodeAttributer = node -> null;

    /**
     * The labeler for edges.
     */
    private Function<Edge<N>, String> edgeLabeler = edge -> null;

    /**
     * Global edge attributes.
     */
    private DotAttributes globalEdgeAttrs = DotAttributes.of();

    /**
     * The function that maps an edge to its attributes.
     */
    private Function<Edge<N>, DotAttributes> edgeAttributer = edge -> null;

    public DotDumper<N> setNodeToString(Function<N, String> nodeToString) {
        this.nodeToString = nodeToString;
        return this;
    }

    public DotDumper<N> setGlobalNodeAttributes(DotAttributes attrs) {
        globalNodeAttrs = attrs;
        return this;
    }

    public DotDumper<N> setNodeLabeler(Function<N, String> nodeLabeler) {
        this.nodeLabeler = nodeLabeler;
        return this;
    }

    public DotDumper<N> setNodeAttributer(
            Function<N, DotAttributes> nodeAttributer) {
        this.nodeAttributer = nodeAttributer;
        return this;
    }

    public DotDumper<N> setEdgeLabeler(Function<Edge<N>, String> edgeLabeler) {
        this.edgeLabeler = edgeLabeler;
        return this;
    }

    public DotDumper<N> setGlobalEdgeAttributes(DotAttributes attrs) {
        globalEdgeAttrs = attrs;
        return this;
    }

    public DotDumper<N> setEdgeAttributer(Function<Edge<N>, DotAttributes> edgeAttributer) {
        this.edgeAttributer = edgeAttributer;
        return this;
    }

    public void dump(Graph<N> graph, File output) {
        try (PrintStream out = new PrintStream(new FileOutputStream(output))) {
            this.out = out;
            // dump starts
            out.println("digraph G {");
            // dump global node attributes
            out.printf("%snode [%s];%n", INDENT, globalNodeAttrs);
            // dump global edge attributes
            out.printf("%sedge [%s];%n", INDENT, globalEdgeAttrs);
            // dump nodes
            graph.forEach(this::dumpNode);
            // dump edges
            graph.forEach(n -> graph.getOutEdgesOf(n).forEach(this::dumpEdge));
            // dump ends
            out.println("}");
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump graph to {}", output.getAbsolutePath(), e);
        }
    }

    private void dumpNode(N node) {
        dumpElement(node, this::nodeToString, nodeLabeler, nodeAttributer);
    }

    private String nodeToString(N node) {
        return "\"" + nodeToString.apply(node) + "\"";
    }

    private void dumpEdge(Edge<N> edge) {
        dumpElement(edge, this::getEdgeRep, edgeLabeler, edgeAttributer);
    }

    private String getEdgeRep(Edge<N> edge) {
        return nodeToString(edge.source()) + " -> " + nodeToString(edge.target());
    }

    /**
     * Dumps an element (either a node or an edge).
     *
     * @param elem       element to be dumped
     * @param toString   function that returns string representation of {@code elem}
     * @param labeler    function that returns label of {@code elem}
     * @param attributer function that returns attributes of {@code elem}
     * @param <T>        type of the element
     */
    private <T> void dumpElement(T elem,
                                 Function<T, String> toString,
                                 Function<T, String> labeler,
                                 Function<T, DotAttributes> attributer) {
        out.print(INDENT);
        out.print(toString.apply(elem));
        String label = labeler.apply(elem);
        DotAttributes attrs = attributer.apply(elem);
        if (label != null || attrs != null) {
            out.print(" [");
            if (label != null) {
                out.printf("label=\"%s\",", label);
            }
            if (attrs != null) {
                out.print(attrs);
            }
            out.print(']');
        }
        out.println(';');
    }
}
