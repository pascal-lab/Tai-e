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

package pascal.taie.analysis.dfa.analysis;

import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.analysis.dfa.fact.NodeResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Stmt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Special class for printing the analysis results of other analyses.
 * This analysis should be placed after all analyses it prints
 * in the analysis plan.
 */
public class ResultPrinter extends IntraproceduralAnalysis {

    public static final String ID = "print-result";

    public ResultPrinter(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze(IR ir) {
        ((List<String>) getOptions().get("analyses"))
                .forEach(id -> printResult(ir, id));
        return null;
    }

    private static void printResult(IR ir, String id) {
        System.out.printf("-------------------- %s (%s) --------------------%n",
                ir.getMethod(), id);
        Object result = ir.getResult(id);
        if (result instanceof Set) {
            ((Set<?>) result).forEach(e -> System.out.println(toString(e)));
        } else if (result instanceof Map) {
            ((Map<?,?>) result).forEach((k, v) ->
                    System.out.println(toString(k) + "=" + toString(v)));
        } else if (result instanceof NodeResult) {
            NodeResult<Stmt, ?> nodeResult = (NodeResult<Stmt, ?>) result;
            ir.getStmts().forEach(stmt ->
                    System.out.println(toString(stmt) + " " +
                            toString(nodeResult.getOutFact(stmt))));
        } else {
            System.out.println(toString(result));
        }
        System.out.println();
    }

    /**
     * Converts an object to string representation.
     * Here we specially handle Stmt by calling IRPrint.toString().
     */
    private static String toString(Object o) {
        if (o instanceof Stmt) {
            return IRPrinter.toString((Stmt) o);
        } else {
            return Objects.toString(o);
        }
    }
}
