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

package pascal.taie.ir;

import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;

import java.io.PrintStream;
import java.util.stream.Collectors;

public class IRPrinter {

    public static void print(NewIR ir, PrintStream out) {
        // print method signature
        out.println(ir.getMethod());
        // print parameters
        out.print("Parameters: ");
        out.println(ir.getParams()
                .stream()
                .map(p -> p.getType() + " " + p)
                .collect(Collectors.joining(", ")));
        // print all variables
        out.println("Variables:");
        ir.getVars().forEach(v -> out.println(v.getType() + " " + v));
        // print all statements
        out.println("Statements:");
        ir.getStmts().forEach(s -> {
            if (s instanceof SwitchStmt) {
                out.println(toString((SwitchStmt) s));
            } else {
                out.println(toString(s));
            }
        });
        // print all try-catch blocks
        if (!ir.getExceptionEntries().isEmpty()) {
            out.println("Exception entries:");
            ir.getExceptionEntries().forEach(b -> out.println("  " + b));
        }
    }

    private static String toString(SwitchStmt switchStmt) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%4d@L%-4d: %s(%s){%n",
                switchStmt.getIndex(), switchStmt.getLineNumber(),
                switchStmt.getInsnString(), switchStmt.getValue()));
        switchStmt.getCaseTargets().forEach(caseTarget -> {
            int caseValue = caseTarget.getFirst();
            Stmt target = caseTarget.getSecond();
            sb.append(String.format("              case %d: goto %s;%n",
                    caseValue, switchStmt.toString(target)));
        });
        sb.append(String.format("              default: goto %s;%n",
                switchStmt.toString(switchStmt.getDefaultTarget())));
        sb.append("            };");
        return sb.toString();
    }

    private static String toString(Stmt stmt) {
        return String.format("%4d@L%-4d: %s;",
                stmt.getIndex(), stmt.getLineNumber(), stmt);
    }
}
