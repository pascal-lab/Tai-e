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

public class CFGDumper {

    private static final Logger logger = LogManager.getLogger(CFGDumper.class);

    private static final String INDENT = "  ";

    private static final String NODE_ATTR = "[shape=box,style=filled,color=\".3 .2 1.0\"]";

    public static void dumpDotFile(CFG<Stmt> cfg, String filePath) {
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(filePath))) {
            dumpDot(cfg, out);
        } catch (FileNotFoundException e) {
            logger.warn("Fail to dump graph to " + filePath +
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
                        out.printf("%s\"%s\" -> \"%s\" [label=\"%s\"];%n",
                                INDENT, toString(s, cfg),
                                toString(e.getTarget(), cfg), e.getKind())));
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
}
