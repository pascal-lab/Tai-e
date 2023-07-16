package pascal.taie.frontend.newfrontend;

import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.language.classes.JMethod;

import java.util.concurrent.atomic.AtomicLong;

class Printer {
    public static void printTestRes(boolean includeAssignLiteral) {
        AtomicLong stmtCount = new AtomicLong();
        AtomicLong varCount = new AtomicLong();
        AtomicLong assignLiteralCount = new AtomicLong();

        World.get()
                .getClassHierarchy()
                .allClasses()
                .forEach(c -> {
                    for (JMethod m : c.getDeclaredMethods()) {
                        if (!m.isAbstract()) {
                            IR ir = m.getIR();
                            stmtCount.addAndGet(ir.getStmts().size());
                            varCount.addAndGet(ir.getVars().size());
                            if (!includeAssignLiteral) {
                                for (var i : ir.getStmts()) {
                                    if (i instanceof AssignLiteral) {
                                        assignLiteralCount.incrementAndGet();
                                    }
                                }
                            }
                        }
                    }
                });

        System.out.println("Count of all the stmts: " + stmtCount.get());
        System.out.println("Count of all the stmts except AssignLiterals: " + (stmtCount.get() - assignLiteralCount.get()));
        System.out.println("Count of all the vars: " + varCount.get());
    }
}
