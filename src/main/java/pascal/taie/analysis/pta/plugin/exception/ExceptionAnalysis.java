package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.exception.CatchAnalysis;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeManager;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static pascal.taie.util.collection.Sets.newHybridSet;

public class ExceptionAnalysis implements Plugin {

    private Solver solver;

    /**
     * Map from thrown variables to the corresponding throw statements.
     */
    private final Map<Var, Set<Throw>> var2Throws = Maps.newMap();

    /**
     * Map from each method to the result of catch analysis on it.
     */
    private final Map<JMethod, Map<Stmt, List<ExceptionEntry>>> catchers =
            Maps.newMap(1024);

    /**
     * Work-list of exception entries to be propagated.
     */
    private final ExceptionWorkList workList = new ExceptionWorkList();

    private CSManager csManager;

    private TypeManager typeManager;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.csManager = solver.getCSManager();
        this.typeManager = solver.getTypeManager();
    }

    /**
     * Establishes the map from all exception references to related throw
     * statements in the new-reached method, and as for the throw statements in
     * the method, analyzes and records all the exception entries that handle
     * the exceptions thrown by the statements.
     *
     * @param method the method that the solver meet now
     */
    @Override
    public void onNewMethod(JMethod method) {
        IR ir = method.getIR();
        ir.forEach(stmt -> {
            if (stmt instanceof Throw) {
                Throw throwStmt = (Throw) stmt;
                Var exceptionRef = throwStmt.getExceptionRef();
                Maps.addToMapSet(var2Throws, exceptionRef, throwStmt);
            }
        });
        catchers.put(method, CatchAnalysis.getPotentialCatchers(ir));
    }

    /**
     * If the csVar is an exception reference, propagate all the exception
     * it newly throws.
     *
     * @param csVar variable pointer
     * @param pts   objects added to the csVar points to set
     */
    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Set<Throw> throwStmts = var2Throws.get(csVar.getVar());
        if (throwStmts != null) {
            Var exceptionRef = csVar.getVar();
            Context ctx = csVar.getContext();
            JMethod currentMethod = exceptionRef.getMethod();
            CSMethod currentCSMethod = csManager.getCSMethod(ctx, currentMethod);
            throwStmts.forEach(throwStmt ->
                    workList.addEntry(currentCSMethod, throwStmt,
                            new ArrayList<>(pts.getObjects())));
            propagateExceptions();
        }
    }

    /**
     * For a new call edge, the exception thrown by the callee method should be
     * propagated to its callers, and thrown by the invoke statement,
     * then we propagate the thrown exceptions accordingly.
     *
     * @param edge the newly established call edge
     */
    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge.getKind() != CallKind.OTHER) {
            // currently, don't propagate exceptions along OTHER edges
            CSMethod callee = edge.getCallee();
            Optional<CSMethodThrowResult> csResult =
                    callee.getResult(getClass().getName());
            csResult.ifPresent(result -> {
                CSMethod caller = edge.getCallSite().getContainer();
                Invoke invoke = edge.getCallSite().getCallSite();
                Collection<CSObj> exceptions = result.mayThrowUncaught();
                workList.addEntry(caller, invoke, exceptions);
                propagateExceptions();
            });
        }
    }

    /**
     * Propagates exceptions from callees to callers (and callers' caller).
     * When a statement throws new exceptions, call {@link #analyzeIntraUncaught}
     * to handle the exceptions that can be caught by the containing method.
     * If there are uncaught exceptions in the method, they will be propagated
     * to call site (invoke) of the method.
     */
    private void propagateExceptions() {
        while (!workList.isEmpty()) {
            ExceptionWorkList.Entry entry = workList.pollEntry();
            CSMethod csMethod = entry.csMethod;
            Stmt stmt = entry.stmt;
            Collection<CSObj> exceptions = entry.exceptions;
            CSMethodThrowResult result = csMethod.getResult(
                    getClass().getName(), CSMethodThrowResult::new);
            Collection<CSObj> diff = result.propagate(stmt, exceptions);
            if (!diff.isEmpty()) {
                Collection<CSObj> uncaught = analyzeIntraUncaught(
                        stmt, diff, csMethod);
                if (!uncaught.isEmpty()) {
                    result.addUncaughtExceptions(uncaught);
                    solver.getCallGraph()
                            .edgesTo(csMethod)
                            // currently, don't propagate exceptions along OTHER edges
                            .filter(edge -> edge.getKind() != CallKind.OTHER)
                            .forEach(edge -> {
                                CSCallSite callSite = edge.getCallSite();
                                CSMethod caller = callSite.getContainer();
                                Invoke invoke = callSite.getCallSite();
                                workList.addEntry(caller, invoke, uncaught);
                            });
                }
            }
        }
    }

    /**
     * Performs an intra-procedural analysis to compute the exceptions that are
     * not caught by the current method.
     *
     * @param currentStmt   the statements that throws exceptions
     * @param newExceptions the new-found exceptions thrown by currentStmt
     * @param csMethod      the csMethod containing currentStmt
     * @return the exceptions thrown by currentStmt but not caught by csMethod
     */
    private Collection<CSObj> analyzeIntraUncaught(
            Stmt currentStmt,
            Collection<CSObj> newExceptions,
            CSMethod csMethod) {
        List<ExceptionEntry> entries = catchers.get(csMethod.getMethod())
                .get(currentStmt);
        if (entries != null) {
            Context ctx = csMethod.getContext();
            for (ExceptionEntry entry : entries) {
                Collection<CSObj> uncaughtExceptions = newHybridSet();
                newExceptions.forEach(newException -> {
                    Obj exObj = newException.getObject();
                    if (typeManager.isSubtype(entry.getCatchType(), exObj.getType())) {
                        Catch catchStmt = entry.getHandler();
                        Var exceptionRef = catchStmt.getExceptionRef();
                        solver.addVarPointsTo(ctx, exceptionRef, newException);
                    } else {
                        uncaughtExceptions.add(newException);
                    }
                });
                newExceptions = uncaughtExceptions;
            }
        }
        return newExceptions;
    }

    @Override
    public void onFinish() {
        // Collects context-sensitive throw results and stores them in
        // a context-insensitive manner.
        PTAThrowResult throwResult = new PTAThrowResult();
        solver.getCallGraph()
                .reachableMethods()
                .forEach(csMethod -> {
                    JMethod method = csMethod.getMethod();
                    MethodThrowResult result = throwResult.getOrCreateResult(method);
                    Optional<CSMethodThrowResult> csResult =
                            csMethod.getResult(getClass().getName());
                    csResult.ifPresent(result::addCSMethodThrowResult);
                });
        solver.getResult()
                .storeResult(getClass().getName(), throwResult);
    }
}
