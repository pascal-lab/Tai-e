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

package pascal.taie.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.StreamUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods about call graph.
 */
public class CGUtils {

    private static final Logger logger = LogManager.getLogger(CGUtils.class);

    /**
     * Separator between call site and its callees.
     */
    private static final String SEP = " -> ";

    public static CallKind getCallKind(InvokeExp invokeExp) {
        if (invokeExp instanceof InvokeVirtual) {
            return CallKind.VIRTUAL;
        } else if (invokeExp instanceof InvokeInterface) {
            return CallKind.INTERFACE;
        } else if (invokeExp instanceof InvokeSpecial) {
            return CallKind.SPECIAL;
        } else if (invokeExp instanceof InvokeStatic) {
            return CallKind.STATIC;
        } else if (invokeExp instanceof InvokeDynamic) {
            return CallKind.DYNAMIC;
        } else {
            throw new AnalysisException("Cannot handle InvokeExp: " + invokeExp);
        }
    }

    public static CallKind getCallKind(Invoke invoke) {
        return getCallKind(invoke.getInvokeExp());
    }

    static void dumpCallGraph(CallGraph<Invoke, JMethod> callGraph, String output) {
        File outFile = new File(output);
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping call graph to {} ...", outFile);
            Comparator<JMethod> cmp = Comparator.comparing(JMethod::toString);
            out.printf("#reachable methods: %d%n", callGraph.getNumberOfMethods());
            out.println("---------- Reachable methods: ----------");
            callGraph.reachableMethods()
                    .sorted(cmp)
                    .forEach(out::println);
            out.printf("%n#call graph edges: %d%n", callGraph.getNumberOfEdges());
            out.println("---------- Call graph edges: ----------");
            callGraph.reachableMethods()
                    .sorted(cmp) // sort reachable methods
                    .forEach(caller ->
                            callGraph.callSitesIn(caller)
                                    .sorted(Comparator.comparing(Invoke::getIndex))
                                    .filter(callSite ->
                                            !StreamUtils.isEmpty(callGraph.calleesOf(callSite)))
                                    .forEach(callSite ->
                                            out.println(toString(callSite) + SEP +
                                                    toString(callGraph.calleesOf(callSite)))));
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump call graph to " + outFile, e);
        }
    }

    /**
     * Compare a call graph with input file.
     * Current implementation is not efficient, and is mainly for testing purpose.
     * @throws AnalysisException if there are mismatches between given call graph
     *  and the one read from input file.
     */
    static void compareCallGraph(CallGraph<Invoke, JMethod> callGraph, String input) {
        logger.info("Comparing call graph with {} ...", input);
        Map<String, String> inputs = readCallEdges(input);
        // Obtain map from Invoke.toString() to Invoke
        Map<String, Invoke> invokes = new LinkedHashMap<>();
        callGraph.reachableMethods()
                .map(callGraph::callSitesIn)
                .flatMap(callSites -> callSites.sorted(
                        Comparator.comparing(Invoke::getIndex)))
                .forEach(callSite -> invokes.put(toString(callSite), callSite));
        List<String> mismatches = new ArrayList<>();
        invokes.forEach((invokeStr, invoke) -> {
            String given = toString(callGraph.calleesOf(invoke));
            String expected = inputs.get(invokeStr);
            if (!given.equals(expected)) {
                mismatches.add(String.format("%s, expected: %s, given: %s",
                        invokeStr, expected, given));
            }
        });
        inputs.keySet()
                .stream()
                .filter(Predicate.not(invokes::containsKey))
                .forEach(invokeStr -> {
                    String expected = inputs.get(invokeStr);
                    mismatches.add(String.format("%s, expected: %s, given: null",
                            invokeStr, expected));
                });
        if (!mismatches.isEmpty()) {
            throw new AnalysisException("Mismatches of call graph\n" +
                    String.join("\n", mismatches));
        }
    }

    private static Map<String, String> readCallEdges(String input) {
        try {
            Map<String, String> edges = new LinkedHashMap<>();
            Files.lines(Path.of(input))
                    .filter(line -> line.contains(SEP))
                    .map(line -> line.split(SEP))
                    .forEach(s -> edges.put(s[0], s[1]));
            return edges;
        } catch (IOException e) {
            throw new AnalysisException(
                    "Failed to read call graph from file " + input, e);
        }
    }

    private static String toString(Invoke invoke) {
        return invoke.getContainer() + IRPrinter.toString(invoke);
    }

    private static String toString(Stream<JMethod> methods) {
        return methods
                .sorted(Comparator.comparing(JMethod::toString))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .toString();
    }
}
