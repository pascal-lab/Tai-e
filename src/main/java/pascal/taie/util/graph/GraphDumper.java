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
import java.util.function.Function;

public class GraphDumper {

    private static final Logger logger = LogManager.getLogger(GraphDumper.class);

    private static final String INDENT = "  ";

    private static final String NODE_ATTR = "[shape=box,style=filled,color=\".3 .3 1.0\"]";

    private GraphDumper() {}

    public static <N> void dump(Graph<N> graph, PrintStream out) {
        graph.nodes()
                .forEach(s -> graph.succsOf(s)
                        .forEach(t -> out.println(s + " -> " + t)));
    }

    public static <N> void dumpDotFile(Graph<N> graph, String filePath) {
        dumpDotFile(graph, filePath, N::toString);
    }

    /**
     *
     * @param graph    the graph to be dumped
     * @param filePath the path of output file
     * @param f        function converting graph node to string representation
     * @param <N> type of graph node
     */
    public static <N> void dumpDotFile(Graph<N> graph, String filePath,
                                       Function<N, String> f) {
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(filePath))) {
            dumpDot(graph, out, f);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump graph to " + filePath +
                    ", caused by  " + e);
        }
    }

    private static <N> void dumpDot(Graph<N> graph, PrintStream out,
                                    Function<N, String> f) {
        out.println("digraph G {");
        // set node style
        out.printf("%snode %s;%n", INDENT, NODE_ATTR);
        // dump nodes
        graph.nodes().forEach(n ->
                out.printf("%s\"%s\";%n", INDENT, f.apply(n)));
        // dump edges
        graph.nodes().forEach(s -> graph.succsOf(s).forEach(t ->
                out.printf("%s\"%s\" -> \"%s\";%n",
                        INDENT, f.apply(s), f.apply(t))));
        out.println("}");
    }
}
