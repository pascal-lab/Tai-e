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

        World.get()
                .getClassHierarchy()
                .allClasses()
                .forEach(c -> {
                    for (JMethod m : c.getDeclaredMethods()) {
                        if (!m.isAbstract()) {
                            IR ir = m.getIR();
                            if (includeAssignLiteral) {
                                stmtCount.addAndGet(ir.getStmts().size());
                            } else {
                                int assignLiteralCount = 0;
                                for (var i : ir.getStmts()) {
                                    if (i instanceof AssignLiteral) {
                                        assignLiteralCount++;
                                    }
                                }
                                stmtCount.addAndGet(ir.getStmts().size() - assignLiteralCount);
                            }
                            varCount.addAndGet(ir.getVars().size());
                        }
                    }
                });

        System.out.println("Count of all the stmts"
                + (includeAssignLiteral ? "" : " except AssignLiterals") + ": " + stmtCount.get());
        System.out.println("Count of all the vars: " + varCount.get());
    }
}
