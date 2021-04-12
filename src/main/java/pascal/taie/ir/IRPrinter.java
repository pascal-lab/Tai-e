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
import pascal.taie.ir.stmt.LookupSwitch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.TableSwitch;

import java.io.PrintStream;
import java.util.stream.Collectors;

public class IRPrinter {

    public static void print(IR ir, PrintStream out) {
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
        StmtVisitor stmtPrinter = new StmtPrinter(out);
        ir.getStmts().forEach(s -> s.accept(stmtPrinter));
        // print all try-catch blocks
        if (!ir.getExceptionEntries().isEmpty()) {
            out.println("Exception entries:");
            ir.getExceptionEntries().forEach(b -> out.println("  " + b));
        }
    }

    private static class StmtPrinter implements StmtVisitor {

        private final PrintStream out;

        private StmtPrinter(PrintStream out) {
            this.out = out;
        }

        @Override
        public void visit(TableSwitch stmt) {
            printSwitch(stmt);
        }

        @Override
        public void visit(LookupSwitch stmt) {
            printSwitch(stmt);
        }

        private void printSwitch(SwitchStmt switchStmt) {
            out.printf("%4d@L%-4d: %s(%s){%n",
                    switchStmt.getIndex(), switchStmt.getLineNumber(),
                    switchStmt.getInsnString(), switchStmt.getValue());
            switchStmt.getCaseTargets().forEach(caseTarget -> {
                int caseValue = caseTarget.getFirst();
                Stmt target = caseTarget.getSecond();
                out.printf("              case %d: goto %s;%n",
                        caseValue, switchStmt.toString(target));
            });
            out.printf("              default: goto %s;%n",
                    switchStmt.toString(switchStmt.getDefaultTarget()));
            out.println("            };");
        }

        @Override
        public void visit(Invoke stmt) {
            out.printf("%4d@L%-4d: ", stmt.getIndex(), stmt.getLineNumber());
            if (stmt.getResult() != null) {
                out.print(stmt.getResult() + " = ");
            }
            InvokeExp ie = stmt.getInvokeExp();
            out.print(ie.getInvokeString());
            out.print(' ');
            if (ie instanceof InvokeDynamic) {
                InvokeDynamic indy = (InvokeDynamic) ie;
                out.printf("%s \"%s\" <%s>[%s]%s;%n",
                        indy.getBootstrapMethodRef(),
                        indy.getMethodName(), indy.getMethodType(),
                        indy.getBootstrapArgs().stream()
                                .map(Literal::toString)
                                .collect(Collectors.joining(",")),
                        indy.getArgsString());
            } else {
                if (ie instanceof InvokeInstanceExp) {
                    out.print(((InvokeInstanceExp) ie).getBase().getName());
                    out.print('.');
                }
                out.printf("%s%s;%n", ie.getMethodRef(), ie.getArgsString());
            }
        }

        @Override
        public void visitDefault(Stmt stmt) {
            out.printf("%4d@L%-4d: %s;%n",
                    stmt.getIndex(), stmt.getLineNumber(), stmt);
        }
    }
}
