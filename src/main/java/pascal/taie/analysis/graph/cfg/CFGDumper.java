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

import pascal.taie.config.Configs;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.IDProvider;
import pascal.taie.util.MapIDProvider;
import pascal.taie.util.graph.DotDumper;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

public class CFGDumper {

    /**
     * Limits length of file name, otherwise it may exceed the max file name
     * length of the underlying file system.
     */
    private static final int FILENAME_LIMIT = 200;

    /**
     * Dumps the given CFG to .dot file.
     */
    static <N> void dumpDotFile(CFG<N> cfg) {
        IDProvider<N> provider = new MapIDProvider<>();
        new DotDumper<N>()
                .setNodeToString(n -> Integer.toString(provider.getID(n)))
                .setNodeLabeler(n -> toLabel(n, cfg))
                .setGlobalNodeAttributes(Map.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeLabeler(e -> {
                    Edge<N> edge = (Edge<N>) e;
                    if (edge.isSwitchCase()) {
                        return edge.getKind() +
                                "\n[case " + edge.getCaseValue() + "]";
                    } else if (edge.isExceptional()) {
                        return edge.getKind() + "\n" +
                                edge.getExceptions()
                                        .stream()
                                        .map(t -> t.getJClass().getSimpleName())
                                        .toList();
                    } else {
                        return edge.getKind().toString();
                    }
                })
                .setEdgeAttrs(e -> {
                    if (((Edge<N>) e).isExceptional()) {
                        return Map.of("color", "red");
                    } else {
                        return Map.of();
                    }
                })
                .dump(cfg, toDotPath(cfg));
    }

    public static <N> String toLabel(N node, CFG<N> cfg) {
        if (cfg.isEntry(node)) {
            return "Entry" + cfg.getMethod();
        } else if (cfg.isExit(node)) {
            return "Exit" + cfg.getMethod();
        } else {
            return node instanceof Stmt ?
                    ((Stmt) node).getIndex() + ": " + node.toString().replace("\"", "\\\"") :
                    node.toString();
        }
    }

    private static String toDotPath(CFG<?> cfg) {
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
        fileName = fileName.replaceAll("[\\[\\]<>]", "_") + ".dot";
        return new File(Configs.getOutputDir(), fileName).toString();
    }
}
