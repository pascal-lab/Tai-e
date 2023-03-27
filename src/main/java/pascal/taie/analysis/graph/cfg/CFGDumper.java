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

package pascal.taie.analysis.graph.cfg;

import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.Indexer;
import pascal.taie.util.SimpleIndexer;
import pascal.taie.util.graph.DotAttributes;
import pascal.taie.util.graph.DotDumper;

import java.io.File;
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
    static <N> void dumpDotFile(CFG<N> cfg, File dumpDir) {
        Indexer<N> indexer = new SimpleIndexer<>();
        new DotDumper<N>()
                .setNodeToString(n -> Integer.toString(indexer.getIndex(n)))
                .setNodeLabeler(n -> toLabel(n, cfg))
                .setGlobalNodeAttributes(DotAttributes.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeLabeler(e -> {
                    CFGEdge<N> edge = (CFGEdge<N>) e;
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
                .setEdgeAttributer(e -> {
                    if (((CFGEdge<N>) e).isExceptional()) {
                        return DotAttributes.of("color", "red");
                    } else {
                        return DotAttributes.of();
                    }
                })
                .dump(cfg, new File(dumpDir, toDotFileName(cfg)));
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

    private static String toDotFileName(CFG<?> cfg) {
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
        return fileName;
    }
}
