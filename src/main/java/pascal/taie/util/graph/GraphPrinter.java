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

import java.io.PrintStream;

public class GraphPrinter {

    private GraphPrinter() {}

    public static <N> void print(Graph<N> graph, PrintStream out) {
        graph.nodes()
                .forEach(s -> graph.succsOf(s)
                        .forEach(t -> out.println(s + " -> " + t)));
    }
}
