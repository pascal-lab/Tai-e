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
import pascal.taie.config.Configs;
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
import pascal.taie.util.IDProvider;
import pascal.taie.util.MapIDProvider;
import pascal.taie.util.graph.DotDumper;

import java.io.File;
import java.util.Map;
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
        IDProvider<JMethod> provider = new MapIDProvider<>();
        new DotDumper<JMethod>()
                .setNodeToString(n -> Integer.toString(provider.getID(n)))
                .setNodeLabeler(JMethod::toString)
                .setGlobalNodeAttributes(Map.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeLabeler(e -> IRPrinter.toString(
                        ((MethodEdge<Invoke, JMethod>) e).callSite()))
                .dump(callGraph, output);
    }

    public static String toString(Invoke invoke) {
        return invoke.getContainer() + IRPrinter.toString(invoke);
    }

}
