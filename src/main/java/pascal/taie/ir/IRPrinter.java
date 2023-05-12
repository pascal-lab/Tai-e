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
            // some variable names contain '%', which will be treated as
            // format specifier by formatter, thus we need to escape it
            String lhs = invoke.getResult().toString().replace("%", "%%");
            formatter.format(lhs + " = ");
        }
        InvokeExp ie = invoke.getInvokeExp();
        formatter.format("%s ", ie.getInvokeString());
        if (ie instanceof InvokeDynamic indy) {
            formatter.format("%s \"%s\" <%s>[%s]%s;",
                    indy.getBootstrapMethodRef(),
                    indy.getMethodName(), indy.getMethodType(),
                    indy.getBootstrapArgs()
                            .stream()
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

    public static String position(Stmt stmt) {
        return "[" +
                stmt.getIndex() +
                "@L" + stmt.getLineNumber() +
                ']';
    }
}
