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

package pascal.taie.analysis.bugfinder;

import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.ExceptionNames;
import pascal.taie.util.collection.Sets;

import java.util.Set;

/**
 * A bug-finding analysis that detects dropped or ignored exceptions in catch blocks.
 *
 * <p>This analysis identifies two common exception handling anti-patterns:
 * <ul>
 *   <li><b>DE_MIGHT_IGNORE</b>: The catch block does not use the caught exception at all,
 *       effectively swallowing it silently.</li>
 *   <li><b>DE_MIGHT_DROP</b>: The catch block throws a new exception but does not chain/wrap
 *       the original caught exception, losing valuable stack trace information.</li>
 * </ul>
 *
 * <h2>Analysis Mechanism</h2>
 * <p>The analysis operates in the following phases:
 * <ol>
 *   <li><b>Identify finally handlers</b>: Detects compiler-generated {@code catch(Throwable)}
 *       blocks that rethrow the exception (finally pattern) and excludes them from analysis.</li>
 *   <li><b>Determine catch block boundaries</b>: Computes reachable statements from each
 *       catch handler using control-flow-aware traversal.</li>
 *   <li><b>Check exception usage</b>: Determines if the caught exception variable is
 *       referenced anywhere in the catch block.</li>
 *   <li><b>Analyze throw behavior</b>: If the exception is unused, checks whether the catch
 *       block throws a new exception and whether it properly wraps the caught exception
 *       (e.g., {@code new WrapperException("msg", caughtException)}).</li>
 * </ol>
 *
 * <h2>Severity</h2>
 * <p>Broad exception types ({@code Error}, {@code Exception}, {@code Throwable},
 * {@code RuntimeException}) are reported as {@code CRITICAL}; specific types as {@code MINOR}.
 *
 * @see BugInstance
 * @see ExceptionEntry
 */
public class DroppedException extends MethodAnalysis<Set<BugInstance>> {

    public static final String ID = "dropped-exception";

    public DroppedException(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<BugInstance> analyze(IR ir) {
        Set<BugInstance> bugInstanceSet = Sets.newHybridSet();

        // Collect all catch handler indices
        Set<Integer> catchHandlerIndices = Sets.newHybridSet();
        // Collect all variables caught by finally handlers (catch Throwable that rethrows)
        Set<Var> finallyHandlerVars = Sets.newHybridSet();

        for (ExceptionEntry e : ir.getExceptionEntries()) {
            catchHandlerIndices.add(e.handler().getIndex());
        }

        // First pass: identify finally handler variables
        for (ExceptionEntry e : ir.getExceptionEntries()) {
            if (e.catchType().getName().equals(ClassNames.THROWABLE)) {
                Catch catchStmt = e.handler();
                Var caughtVar = catchStmt.getExceptionRef();
                // Check if this catch Throwable will rethrow the exception (finally pattern)
                if (willRethrowVar(ir, e.handler().getIndex() + 1, caughtVar)) {
                    finallyHandlerVars.add(caughtVar);
                }
            }
        }

        for (ExceptionEntry entry : ir.getExceptionEntries()) {
            String exceptionName = entry.catchType().getName();
            Stmt catchHandler = entry.handler();

            // Get the caught exception variable
            Var caughtException = null;
            if (catchHandler instanceof Catch catchStmt) {
                caughtException = catchStmt.getExceptionRef();
            }

            // Skip finally handlers (catch Throwable that rethrows)
            if (caughtException != null && finallyHandlerVars.contains(caughtException)) {
                continue;
            }

            // Find catch block range
            int catchBlockStart = catchHandler.getIndex() + 1;
            int catchBlockEnd = findCatchBlockEnd(ir, catchBlockStart, catchHandlerIndices);

            // Check if the caught exception variable is used in the catch block
            boolean exceptionUsed = isExceptionUsed(ir, catchBlockStart, catchBlockEnd, caughtException);

            if (!exceptionUsed) {
                // Exception is not used - either ignored or dropped

                // Check if this catch block throws a new exception (not finally rethrow)
                // Also check if the thrown exception wraps the caught exception
                ThrowAnalysisResult throwResult = analyzeThrows(ir, catchBlockStart, catchBlockEnd,
                        caughtException, finallyHandlerVars);

                if (throwResult.hasOwnThrow && !throwResult.exceptionWrapped) {
                    // Has throw but doesn't use or wrap caught exception -> drops exception
                    Severity severity = getSeverity(exceptionName);
                    bugInstanceSet.add(new BugInstance(
                            BugType.DE_MIGHT_DROP, severity, ir.getMethod())
                            .setSourceLine(catchHandler.getLineNumber()));
                } else if (!throwResult.hasOwnThrow) {
                    // No throw or only finally rethrow -> empty/ignored catch block
                    Severity severity = getSeverity(exceptionName);
                    bugInstanceSet.add(new BugInstance(
                            BugType.DE_MIGHT_IGNORE, severity, ir.getMethod())
                            .setSourceLine(catchHandler.getLineNumber()));
                }
                // If exceptionWrapped is true, the exception is properly chained, no bug
            }
        }

        return bugInstanceSet;
    }

    /**
     * Checks if the code starting from {@code startIndex} will eventually throw
     * the specified variable.
     * <p>
     * This method identifies the finally rethrow pattern by performing a control-flow-aware
     * traversal. It handles branches (if/switch) and jumps (goto) to determine if the
     * exception variable will be rethrown along any execution path.
     *
     * @param ir           the intermediate representation of the method
     * @param startIndex   the starting statement index
     * @param exceptionVar the exception variable to check for rethrowing
     * @return {@code true} if the code will rethrow the exception variable
     */
    private boolean willRethrowVar(IR ir, int startIndex, Var exceptionVar) {
        Set<Integer> visited = Sets.newHybridSet();
        return willRethrowVarHelper(ir, startIndex, exceptionVar, visited);
    }

    /**
     * Helper method for {@link #willRethrowVar} that tracks visited indices to handle loops.
     * <p>
     * Uses a shared visited set across recursive calls to ensure O(n) complexity and
     * prevent infinite loops when analyzing cyclic control flow.
     *
     * @param ir           the intermediate representation of the method
     * @param current      the current statement index being analyzed
     * @param exceptionVar the exception variable to check for rethrowing
     * @param visited      set of already visited statement indices
     * @return {@code true} if the code will rethrow the exception variable from this point
     */
    private boolean willRethrowVarHelper(IR ir, int current, Var exceptionVar, Set<Integer> visited) {
        while (current < ir.getStmts().size()) {
            if (visited.contains(current)) {
                return false; // Already visited, avoid infinite loops
            }
            visited.add(current);

            Stmt stmt = ir.getStmt(current);

            if (stmt instanceof Throw throwStmt) {
                return throwStmt.getExceptionRef().equals(exceptionVar);
            }
            if (stmt instanceof Return) {
                return false;
            }
            if (stmt instanceof Goto gotoStmt) {
                current = gotoStmt.getTarget().getIndex();
            } else if (stmt instanceof If ifStmt) {
                // Check both branches with shared visited set
                boolean trueBranch = willRethrowVarHelper(ir, ifStmt.getTarget().getIndex(),
                        exceptionVar, visited);
                boolean falseBranch = willRethrowVarHelper(ir, current + 1,
                        exceptionVar, visited);
                return trueBranch || falseBranch;
            } else if (stmt instanceof SwitchStmt switchStmt) {
                // Check all switch targets with shared visited set
                for (Stmt target : switchStmt.getTargets()) {
                    if (willRethrowVarHelper(ir, target.getIndex(), exceptionVar, visited)) {
                        return true;
                    }
                }
                return willRethrowVarHelper(ir, switchStmt.getDefaultTarget().getIndex(),
                        exceptionVar, visited);
            } else {
                current++;
            }
        }
        return false;
    }

    /**
     * Finds the end index of a catch block.
     * <p>
     * This method handles branches within catch blocks by computing all reachable statements
     * starting from the catch handler. The catch block is considered to end at the maximum
     * reachable index plus one.
     *
     * @param ir                  the intermediate representation of the method
     * @param startIndex          the starting index of the catch block (after the catch statement)
     * @param catchHandlerIndices set of all catch handler indices in the method
     * @return the exclusive end index of the catch block
     */
    private int findCatchBlockEnd(IR ir, int startIndex, Set<Integer> catchHandlerIndices) {
        Set<Integer> reachable = Sets.newHybridSet();
        findReachableStatements(ir, startIndex, catchHandlerIndices, reachable);

        if (reachable.isEmpty()) {
            return startIndex;
        }

        // Return one past the maximum reachable index
        int maxIndex = reachable.stream().mapToInt(Integer::intValue).max().orElse(startIndex);
        return maxIndex + 1;
    }

    /**
     * Finds all statements reachable from the given index within a catch block.
     * <p>
     * The traversal stops at:
     * <ul>
     *   <li>Method boundaries (negative indices or beyond statement list)</li>
     *   <li>Already visited statements (to handle loops)</li>
     *   <li>Other catch handlers (to avoid crossing catch block boundaries)</li>
     *   <li>Terminal statements (return, throw)</li>
     * </ul>
     *
     * @param ir                  the intermediate representation of the method
     * @param index               the current statement index
     * @param catchHandlerIndices set of all catch handler indices
     * @param reachable           output set collecting all reachable statement indices
     */
    private void findReachableStatements(IR ir, int index, Set<Integer> catchHandlerIndices,
                                         Set<Integer> reachable) {
        if (index < 0 || index >= ir.getStmts().size()) {
            return;
        }
        if (reachable.contains(index)) {
            return; // Already visited
        }
        if (catchHandlerIndices.contains(index) && !reachable.isEmpty()) {
            return; // Hit another catch handler (but allow starting at a catch handler)
        }

        reachable.add(index);
        Stmt stmt = ir.getStmt(index);

        if (stmt instanceof Return || stmt instanceof Throw) {
            // Terminal statements - don't continue
            return;
        } else if (stmt instanceof Goto gotoStmt) {
            findReachableStatements(ir, gotoStmt.getTarget().getIndex(),
                    catchHandlerIndices, reachable);
        } else if (stmt instanceof If ifStmt) {
            // Both branches are reachable
            findReachableStatements(ir, ifStmt.getTarget().getIndex(),
                    catchHandlerIndices, reachable);
            findReachableStatements(ir, index + 1, catchHandlerIndices, reachable);
        } else if (stmt instanceof SwitchStmt switchStmt) {
            // All switch targets are reachable
            for (Stmt target : switchStmt.getTargets()) {
                findReachableStatements(ir, target.getIndex(),
                        catchHandlerIndices, reachable);
            }
            findReachableStatements(ir, switchStmt.getDefaultTarget().getIndex(),
                    catchHandlerIndices, reachable);
        } else {
            // Fall through to next statement
            findReachableStatements(ir, index + 1, catchHandlerIndices, reachable);
        }
    }

    /**
     * Checks if the caught exception variable is used anywhere in the catch block.
     * <p>
     * A variable is considered "used" if it appears in the uses set of any statement
     * (excluding Nop statements) within the catch block range.
     *
     * @param ir              the intermediate representation of the method
     * @param startIndex      the inclusive start index of the catch block
     * @param endIndex        the exclusive end index of the catch block
     * @param caughtException the caught exception variable to check
     * @return {@code true} if the exception variable is used in the catch block
     */
    private boolean isExceptionUsed(IR ir, int startIndex, int endIndex, Var caughtException) {
        if (caughtException == null) {
            return false;
        }

        for (int i = startIndex; i < endIndex && i < ir.getStmts().size(); i++) {
            Stmt stmt = ir.getStmt(i);
            // Skip Nop statements
            if (stmt instanceof Nop) {
                continue;
            }
            // Check if any statement uses the caught exception variable
            if (stmt.getUses().contains(caughtException)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Result of analyzing throw statements in a catch block.
     * <p>
     * This record captures two key pieces of information needed to determine
     * the type of exception handling bug (if any).
     */
    private static class ThrowAnalysisResult {
        /**
         * Whether the catch block has its own throw statement (not a finally rethrow)
         */
        final boolean hasOwnThrow;
        /**
         * Whether the thrown exception wraps/chains the caught exception
         */
        final boolean exceptionWrapped;

        ThrowAnalysisResult(boolean hasOwnThrow, boolean exceptionWrapped) {
            this.hasOwnThrow = hasOwnThrow;
            this.exceptionWrapped = exceptionWrapped;
        }
    }

    /**
     * Analyzes throw statements in the catch block to determine if exceptions are properly handled.
     * <p>
     * This method performs two analyses:
     * <ol>
     *   <li><b>Exception wrapping detection</b>: Identifies variables that are created with the
     *       caught exception as a constructor or method argument. This covers patterns like:
     *       <pre>{@code new WrapperException("message", caughtException)}</pre></li>
     *   <li><b>Throw statement analysis</b>: For each throw statement (excluding finally rethrows),
     *       checks if it throws a variable that wraps the caught exception.</li>
     * </ol>
     *
     * @param ir                 the intermediate representation of the method
     * @param startIndex         the inclusive start index of the catch block
     * @param endIndex           the exclusive end index of the catch block
     * @param caughtException    the caught exception variable
     * @param finallyHandlerVars set of variables used by finally handlers (to be excluded)
     * @return analysis result indicating whether the block has own throws and if exceptions are wrapped
     */
    private ThrowAnalysisResult analyzeThrows(IR ir, int startIndex, int endIndex,
                                              Var caughtException, Set<Var> finallyHandlerVars) {
        boolean hasOwnThrow = false;
        boolean exceptionWrapped = false;

        // First, find all variables that are created with the caught exception as an argument
        // (i.e., exception wrapping pattern like: new SomeException("msg", caughtException))
        Set<Var> wrappingExceptions = Sets.newHybridSet();

        if (caughtException != null) {
            for (int i = startIndex; i < endIndex && i < ir.getStmts().size(); i++) {
                Stmt stmt = ir.getStmt(i);
                if (stmt instanceof Invoke invokeStmt) {
                    InvokeExp invokeExp = invokeStmt.getInvokeExp();
                    // Check if this is a constructor call or method that takes caught exception
                    if (invokeExp.getArgs().contains(caughtException)) {
                        // If the result is stored in a variable, track it
                        Var result = invokeStmt.getResult();
                        if (result != null) {
                            wrappingExceptions.add(result);
                        }
                        // Also check if this is an <init> call on a newly created exception
                        // The receiver might be thrown later
                        if (invokeExp.getMethodRef().resolve().isConstructor()) {
                            // For constructor calls: invokespecial SomeException.<init>(this, msg, cause)
                            // for invokespecial on <init>, check the base
                            if (invokeExp instanceof InvokeInstanceExp instanceExp) {
                                wrappingExceptions.add(instanceExp.getBase());
                            }
                        }
                    }
                }
            }
        }

        // Now check throw statements
        for (int i = startIndex; i < endIndex && i < ir.getStmts().size(); i++) {
            Stmt stmt = ir.getStmt(i);
            if (stmt instanceof Throw throwStmt) {
                Var thrownVar = throwStmt.getExceptionRef();

                // Skip finally handler rethrows
                if (finallyHandlerVars.contains(thrownVar)) {
                    continue;
                }

                // This is the catch block's own throw
                hasOwnThrow = true;

                // Check if the thrown exception wraps the caught exception
                if (wrappingExceptions.contains(thrownVar)) {
                    exceptionWrapped = true;
                }
            }
        }

        return new ThrowAnalysisResult(hasOwnThrow, exceptionWrapped);
    }

    /**
     * Determines the severity of a dropped/ignored exception bug based on the exception type.
     * <p>
     * Broad exception types ({@code Error}, {@code Exception}, {@code Throwable},
     * {@code RuntimeException}) are assigned {@link Severity#CRITICAL} because dropping
     * them can hide a wide range of serious issues. More specific exception types are
     * assigned {@link Severity#MINOR}.
     *
     * @param exceptionName the fully qualified name of the caught exception type
     * @return the severity level for bugs involving this exception type
     */
    private Severity getSeverity(String exceptionName) {
        if (exceptionName.equals(ClassNames.ERROR)
                || exceptionName.equals(ClassNames.EXCEPTION)
                || exceptionName.equals(ClassNames.THROWABLE)
                || exceptionName.equals(ExceptionNames.RUNTIME_EXCEPTION)) {
            return Severity.CRITICAL;
        }
        return Severity.MINOR;
    }

    /**
     * Bug types detected by this analysis.
     */
    private enum BugType implements pascal.taie.analysis.bugfinder.BugType {
        /**
         * The catch block throws a new exception without wrapping/chaining
         * the original caught exception, causing loss of error information.
         */
        DE_MIGHT_DROP,

        /**
         * The catch block does not use the caught exception at all,
         * effectively ignoring/swallowing it silently.
         */
        DE_MIGHT_IGNORE
    }

}
