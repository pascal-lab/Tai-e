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
import pascal.taie.config.ConfigUtils;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.util.AnalysisException;
import soot.Unit;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Comparator;

/**
 * Utility methods about call graph.
 */
public class CGUtils {

    private static final Logger logger = LogManager.getLogger(CGUtils.class);

    public static CallKind getCallKind(Unit callSite) {
        InvokeExpr invoke = ((Stmt) callSite).getInvokeExpr();
        return CGUtils.getCallKind(invoke);
    }

    public static CallKind getCallKind(InvokeExpr invoke) {
        if (invoke instanceof InterfaceInvokeExpr) {
            return CallKind.INTERFACE;
        } else if (invoke instanceof VirtualInvokeExpr) {
            return CallKind.VIRTUAL;
        } else if (invoke instanceof SpecialInvokeExpr) {
            return CallKind.SPECIAL;
        } else if (invoke instanceof StaticInvokeExpr) {
            return CallKind.STATIC;
        } else {
            return CallKind.OTHER;
        }
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
        } else {
            throw new AnalysisException("Cannot handle InvokeExp: " + invokeExp);
        }
    }

    static <CallSite, Method> void dumpCallGraph(CallGraph<CallSite, Method> callGraph) {
        File outFile = new File(ConfigUtils.getOutputDir(), "call-graph.txt");
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping call graph to {} ...", outFile);
            out.printf("#reachable methods: %d%n", callGraph.getNumberOfMethods());
            out.println("---------- Reachable methods: ----------");
            callGraph.reachableMethods()
                    .sorted(Comparator.comparing(Method::toString))
                    .forEach(out::println);
            out.printf("%n#call graph edges: %d%n", callGraph.getNumberOfEdges());
            out.println("---------- Call graph edges: ----------");
            callGraph.edges().forEach(out::println);
            out.println("----------------------------------------");
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump call graph to " + outFile, e);
        }
    }
}
