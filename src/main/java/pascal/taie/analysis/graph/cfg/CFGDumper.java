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

package pascal.taie.analysis.graph.cfg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.stmt.Stmt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.stream.Collectors;

public class CFGDumper {

    private static final Logger logger = LogManager.getLogger(CFGDumper.class);

    private static final String INDENT = "  ";

    private static final String NODE_ATTR = "[shape=box,style=filled,color=\".3 .2 1.0\"]";

    private static final String EXCEPTIONAL_EDGE_ATTR = "color=red";

    public static void dumpDotFile(CFG<Stmt> cfg, String filePath) {
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(filePath))) {
            dumpDot(cfg, out);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump graph to " + filePath +
                    ", caused by  " + e);
        }
    }

    private static <N> void dumpDot(CFG<Stmt> cfg, PrintStream out) {
        out.println("digraph G {");
        // set node style
        out.printf("%snode %s;%n", INDENT, NODE_ATTR);
        // dump nodes
        cfg.nodes().forEach(s ->
                out.printf("%s\"%s\";%n", INDENT, toString(s, cfg)));
        // dump edges
        cfg.nodes().forEach(s ->
                cfg.outEdgesOf(s).forEach(e ->
                        out.printf("%s%s;%n", INDENT, toString(e, cfg))));
        out.println("}");
    }

    private static String toString(Stmt s, CFG<Stmt> cfg) {
        if (cfg.isEntry(s)) {
            return "Entry";
        } else if (cfg.isExit(s)) {
            return "Exit";
        } else {
            return s.getIndex() + ": " + s;
        }
    }

    private static String toString(Edge<Stmt> e, CFG<Stmt> cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append('\"').append(toString(e.getSource(), cfg)).append('\"');
        sb.append(" -> ");
        sb.append('\"').append(toString(e.getTarget(), cfg)).append('\"');
        sb.append(" [label=\"").append(e.getKind());
        if (e.isSwitchCase()) {
            sb.append("\n[case ").append(e.getCaseValue()).append(']');
        } else if (e.isExceptional()) {
            sb.append("\n").append(e.exceptions()
                            .map(t -> t.getJClass().getSimpleName())
                            .collect(Collectors.toList()));
        }
        sb.append('\"');
        if (e.isExceptional()) {
            sb.append(',').append(EXCEPTIONAL_EDGE_ATTR);
        }
        sb.append(']');
        return sb.toString();
    }
}
