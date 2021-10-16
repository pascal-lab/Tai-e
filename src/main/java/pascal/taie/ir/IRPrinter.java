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

package pascal.taie.ir;

import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;

import java.io.PrintStream;
import java.util.Formatter;
import java.util.stream.Collectors;

public class IRPrinter {

    public static void print(IR ir, PrintStream out) {
        // print method signature
        out.println("---------- " + ir.getMethod() + " ----------");
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
        ir.forEach(s -> out.println(toString(s)));
        // print all try-catch blocks
        if (!ir.getExceptionEntries().isEmpty()) {
            out.println("Exception entries:");
            ir.getExceptionEntries().forEach(b -> out.println("  " + b));
        }
    }

    public static String toString(Stmt stmt) {
        if (stmt instanceof Invoke) {
            return toString((Invoke) stmt);
        } else {
            return String.format("%s %s;", position(stmt), stmt);
        }
    }

    public static String toString(Invoke invoke) {
        Formatter formatter = new Formatter();
        formatter.format("%s ", position(invoke));
        if (invoke.getResult() != null) {
            formatter.format(invoke.getResult() + " = ");
        }
        InvokeExp ie = invoke.getInvokeExp();
        formatter.format("%s ", ie.getInvokeString());
        if (ie instanceof InvokeDynamic) {
            InvokeDynamic indy = (InvokeDynamic) ie;
            formatter.format("%s \"%s\" <%s>[%s]%s;",
                    indy.getBootstrapMethodRef(),
                    indy.getMethodName(), indy.getMethodType(),
                    indy.getBootstrapArgs().stream()
                            .map(Literal::toString)
                            .collect(Collectors.joining(", ")),
                    indy.getArgsString());
        } else {
            if (ie instanceof InvokeInstanceExp) {
                formatter.format("%s.", ((InvokeInstanceExp) ie).getBase().getName());
            }
            formatter.format("%s%s;", ie.getMethodRef(), ie.getArgsString());
        }
        return formatter.toString();
    }

    private static String position(Stmt stmt) {
        return "[" +
                stmt.getIndex() +
                "@L" + stmt.getLineNumber() +
                ']';
    }
}
