/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.util.graph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        try (PrintStream out = new PrintStream(new FileOutputStream(filePath))) {
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
            graph.forEach(n -> graph.outEdgesOf(n).forEach(this::dumpEdge));
            // dump ends
            out.println("}");
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump graph to {}, caused by {}",
                    filePath, e);
        }
    }

    private void dumpAttributes(Map<String, String> attrs) {
        attrs.forEach((key, value) -> out.printf("%s=%s,", key, value));
    }

    private void dumpNode(N node) {
        out.print(INDENT);
        out.print(getNodeRep(node));
        // dump node attributes
        String label = nodeLabeler.apply(node);
        Map<String, String> attrs = nodeAttrs.apply(node);
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
        out.println(";");
    }

    private String getNodeRep(N node) {
        return "\"" + nodeToString.apply(node) + "\"";
    }

    private void dumpEdge(Edge<N> edge) {
        out.print(INDENT);
        out.printf("%s -> %s",
                getNodeRep(edge.getSource()), getNodeRep(edge.getTarget()));
        String label = edgeLabeler.apply(edge);
        Map<String, String> attrs = edgeAttrs.apply(edge);
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
        out.println(";");
    }
}
