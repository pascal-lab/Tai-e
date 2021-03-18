/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.graph.callgraph.cha;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.frontend.soot.SootUtils;
import soot.Body;
import soot.BodyTransformer;
import soot.BriefUnitPrinter;
import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Print call edges of each invocation Unit.
 */
public class CallGraphPrinter extends BodyTransformer {

    private static final CallGraphPrinter INSTANCE = new CallGraphPrinter();
    private static boolean isOutput = true;
    /**
     * Whether we show empty callees.
     */
    private static boolean isPrintEmpty = false;

    private CallGraphPrinter() {
    }

    public static CallGraphPrinter v() {
        return INSTANCE;
    }

    public static void setOutput(boolean isOutput) {
        CallGraphPrinter.isOutput = isOutput;
    }

    public static void setPrintEmpty(boolean isPrintEmpty) {
        CallGraphPrinter.isPrintEmpty = isPrintEmpty;
    }

    @Override
    protected synchronized void internalTransform(Body b, String phaseName, Map<String, String> options) {
        CallGraph<Unit, SootMethod> callGraph = CHACallGraphBuilder.v()
                .getRecentCallGraph();
        SootMethod m = b.getMethod();
        if (isOutput) {
            boolean hasCallees = false;
            BriefUnitPrinter up = new BriefUnitPrinter(b);
            for (Unit u : b.getUnits()) {
                if (!callGraph.getCallSitesIn(m).contains(u)) {
                    // skips non-call units
                    continue;
                }
                Collection<SootMethod> callees = callGraph.getCallees(u);
                if (!callees.isEmpty() || isPrintEmpty) {
                    if (!hasCallees) {
                        hasCallees = true;
                        System.out.println("------ " + b.getMethod() + " [call graph] -----");
                    }
                    System.out.println(SootUtils.unitToString(up, u)
                            + " -> " + calleesToString(callees));
                }
            }
            if (hasCallees) {
                System.out.println();
            }
        }
        if (ResultChecker.isAvailable()) {
            ResultChecker.get().compare(b, callGraph);
        }
    }

    /**
     * Converts a collection of callees (SootMethods) to a String in format:
     * [M1, M2, ...], where the callees are sorted by their signatures.
     *
     * @return the string representation of callees
     */
    public String calleesToString(Collection<SootMethod> callees) {
        List<SootMethod> calleeList = new ArrayList<>(callees);
        calleeList.sort(Comparator.comparing(SootMethod::toString));
        return calleeList.toString();
    }
}
