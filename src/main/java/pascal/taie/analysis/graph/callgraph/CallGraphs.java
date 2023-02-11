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

package pascal.taie.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.config.Configs;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Indexer;
import pascal.taie.util.SimpleIndexer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.graph.DotDumper;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Static utility methods about call graph.
 */
public final class CallGraphs {

    private static final Logger logger = LogManager.getLogger(CallGraphs.class);

    private CallGraphs() {
    }

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

    @Nullable
    public static JMethod resolveCallee(Type type, Invoke callSite) {
        MethodRef methodRef = callSite.getMethodRef();
        if (callSite.isInterface() || callSite.isVirtual()) {
            return World.get().getClassHierarchy()
                    .dispatch(type, methodRef);
        } else if (callSite.isSpecial()) {
            return World.get().getClassHierarchy()
                    .dispatch(methodRef.getDeclaringClass(), methodRef);
        } else if (callSite.isStatic()) {
            return methodRef.resolveNullable();
        } else {
            throw new AnalysisException("Cannot resolve Invoke: " + callSite);
        }
    }

    /**
     * Dumps call graph to dot file.
     */
    static void dumpCallGraph(CallGraph<Invoke, JMethod> callGraph, String output) {
        if (output == null) {
            output = new File(Configs.getOutputDir(),
                    callGraph.entryMethods()
                            .map(m -> m.getDeclaringClass() + "." + m.getName())
                            .collect(Collectors.joining("-")) + "-cg.dot")
                    .toString();
        }
        logger.info("Dumping call graph to {} ...", output);
        Indexer<JMethod> indexer = new SimpleIndexer<>();
        new DotDumper<JMethod>()
                .setNodeToString(n -> Integer.toString(indexer.getIndex(n)))
                .setNodeLabeler(JMethod::toString)
                .setGlobalNodeAttributes(Map.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeLabeler(e -> IRPrinter.toString(
                        ((MethodEdge<Invoke, JMethod>) e).callSite()))
                .dump(callGraph, output);
    }

    static void dumpMethods(CallGraph<Invoke, JMethod> callGraph, String output) {
        File outFile = new File(output);
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping reachable methods to {} ...", outFile);
            callGraph.reachableMethods()
                    .map(JMethod::getSignature)
                    .sorted()
                    .forEach(out::println);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump reachable methods to " + outFile, e);
        }
    }

    static void dumpCallEdges(CallGraph<Invoke, JMethod> callGraph, String output) {
        File outFile = new File(output);
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping call edges to {} ...", outFile);
            callGraph.reachableMethods()
                    // sort callers
                    .sorted(Comparator.comparing(JMethod::getSignature))
                    .forEach(m -> getInvokeReps(m).forEach((invoke, rep) ->
                            callGraph.getCalleesOf(invoke)
                                    .stream()
                                    .sorted(Comparator.comparing(JMethod::getSignature))
                                    .forEach(callee -> out.println(rep + "\t" + callee))));
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump call graph edges to " + outFile, e);
        }
    }

    /**
     * @return a map from Invoke to its string representation in given method.
     */
    private static Map<Invoke, String> getInvokeReps(JMethod caller) {
        Map<String, Integer> counter = Maps.newMap();
        Map<Invoke, String> invokeReps =
                new TreeMap<>(Comparator.comparing(Invoke::getIndex));
        caller.getIR().forEach(s -> {
            if (s instanceof Invoke invoke) {
                if (invoke.isDynamic()) { // skip invokedynamic
                    return;
                }
                MethodRef ref = invoke.getMethodRef();
                String target = ref.getDeclaringClass().getName() + "." + ref.getName();
                int n = getInvokeNumber(target, counter);
                String rep = caller + "/" + target + "/" + n;
                invokeReps.put(invoke, rep);
            }
        });
        return invokeReps;
    }

    private static int getInvokeNumber(String target, Map<String, Integer> counter) {
        Integer n = counter.get(target);
        if (n == null) {
            n = 0;
        }
        counter.put(target, n + 1);
        return n;
    }

    public static String toString(Invoke invoke) {
        return invoke.getContainer() + IRPrinter.toString(invoke);
    }

    static void dumpDot(CallGraph<Invoke, JMethod> callGraph, String output) {
        logger.info("Dumping call graph to {} ...", output);
        Indexer<JMethod> indexer = new SimpleIndexer<>();
        new DotDumper<JMethod>()
                .setNodeToString(JMethod::getSignature)
                .setNodeLabeler(m -> Integer.toString(indexer.getIndex(m)))
                .dump(callGraph, output);
    }
}
