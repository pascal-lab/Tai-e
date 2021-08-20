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
import pascal.taie.config.Configs;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.stream.Collectors;

public class CFGDumper {

    private static final Logger logger = LogManager.getLogger(CFGDumper.class);

    /**
     * Limits length of file name, otherwise it may exceed the max file name
     * length of the underlying file system.
     */
    private static final int FILENAME_LIMIT = 200;

    private static final String INDENT = "  ";

    private static final String NODE_ATTR = "[shape=box,style=filled,color=\".3 .2 1.0\"]";

    private static final String EXCEPTIONAL_EDGE_ATTR = "color=red";

    static <N> void dumpDotFile(CFG<N> cfg) {
        // obtain output file
        File outFile = new File(Configs.getOutputDir(), toFileName(cfg));
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            dumpDot(cfg, out);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump control-flow graph to " + outFile, e);
        }
    }

    private static String toFileName(CFG<?> cfg) {
        JMethod m = cfg.getMethod();
        String fileName = String.valueOf(m.getDeclaringClass()) + '.' +
                m.getName() + '(' +
                m.getParamTypes()
                        .stream()
                        .map(Type::toString)
                        .collect(Collectors.joining(",")) +
                ')';
        if (fileName.length() > FILENAME_LIMIT) {
            fileName = fileName.substring(0, FILENAME_LIMIT) + "...";
        }
        // escape invalid characters in file name
        return fileName.replaceAll("[\\[\\]<>]", "_") + ".dot";
    }

    private static <N> void dumpDot(CFG<N> cfg, PrintStream out) {
        out.println("digraph G {");
        // set node style
        out.printf("%snode %s;%n", INDENT, NODE_ATTR);
        // dump nodes
        cfg.forEach(s ->
                out.printf("%s\"%s\";%n", INDENT, toString(s, cfg)));
        // dump edges
        cfg.forEach(s ->
                cfg.outEdgesOf(s).forEach(e ->
                        out.printf("%s%s;%n", INDENT, toString(e, cfg))));
        out.println("}");
    }

    private static <N> String toString(N node, CFG<N> cfg) {
        if (cfg.isEntry(node)) {
            return "Entry";
        } else if (cfg.isExit(node)) {
            return "Exit";
        } else {
            return node instanceof Stmt ?
                    ((Stmt) node).getIndex() + ": " + node.toString().replace("\"", "\\\"") :
                    node.toString();
        }
    }

    private static <N> String toString(Edge<N> e, CFG<N> cfg) {
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
