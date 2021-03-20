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

public class GraphDumper {

    private static final Logger logger = LogManager.getLogger(GraphDumper.class);

    private GraphDumper() {}

    public static <N> void dump(Graph<N> graph, PrintStream out) {
        graph.nodes()
                .forEach(s -> graph.succsOf(s)
                        .forEach(t -> out.println(s + " -> " + t)));
    }

    private static final String INDENT = "  ";

    private static <N> void dumpDot(Graph<N> graph, PrintStream out) {
        out.println("digraph G {");
        // set node style
        out.println(INDENT + "node [shape=box,style=filled,color=\".3 .3 1.0\"];");
        // dump nodes
        graph.nodes().forEach(n -> out.printf("%s\"%s\";%n", INDENT, n));
        // dump edges
        graph.nodes().forEach(s -> graph.succsOf(s).forEach(t ->
                out.printf("%s\"%s\" -> \"%s\";%n", INDENT, s, t)));
        out.println("}");
    }

    public static <N> void dumpDotFile(Graph<N> graph, String filePath) {
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(filePath))) {
            dumpDot(graph, out);
        } catch (FileNotFoundException e) {
            logger.warn("Fail to dump graph to " + filePath +
                    ", caused by  " + e);
        }
    }
}
