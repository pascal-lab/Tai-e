package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.exception.CatchAnalysis;
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
import pascal.taie.util.collection.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.SetUtils.newHybridSet;

public class ExceptionAnalysis implements Plugin {

    private Solver solver;

    /**
     * Map from thrown variables to the corresponding throw statements.
     */
    private final Map<Var, Set<Throw>> var2Throws = MapUtils.newMap();

    /**
     * Map from each method to the result of catch analysis on it.
     */
    private final Map<JMethod, Map<Stmt, List<ExceptionEntry>>> catchers =
            MapUtils.newMap(1024);

    private final ExceptionWorkList workList = new ExceptionWorkList();

    private CSManager csManager;

    private TypeManager typeManager;

    private PTAThrowResult throwResult;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.csManager = solver.getCSManager();
        this.typeManager = solver.getTypeManager();
        this.throwResult = solver.getThrowResult();
    }

    @Override
    public void onNewMethod(JMethod method) {
        IR ir = method.getIR();
        ir.getStmts().forEach(stmt -> {
            if (stmt instanceof Throw) {
                Throw throwStmt = (Throw) stmt;
                Var exceptionRef = throwStmt.getExceptionRef();
                MapUtils.addToMapSet(var2Throws, exceptionRef, throwStmt);
            }
        });
        catchers.put(method, CatchAnalysis.getPotentialCatchers(ir));
    }

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

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        CSMethod callee = edge.getCallee();
        callee.getThrowResult().ifPresent(result -> {
            CSMethod caller = edge.getCallSite().getContainer();
            Invoke invoke = edge.getCallSite().getCallSite();
            Collection<CSObj> exceptions = result.mayThrowUncaught();
            workList.addEntry(caller, invoke, exceptions);
            propagateExceptions();
        });
    }

    private void propagateExceptions() {
        while (!workList.isEmpty()) {
            ExceptionWorkList.Entry entry = workList.pollEntry();
            CSMethod csMethod = entry.csMethod;
            Stmt stmt = entry.stmt;
            Collection<CSObj> exceptions = entry.exceptions;
            CSMethodThrowResult result = csMethod.getOrCreateThrowResult();
            Collection<CSObj> diff = result.propagate(stmt, exceptions);
            if (!diff.isEmpty()) {
                Collection<CSObj> uncaught = analyzeIntraUncaught(
                        stmt, diff, csMethod);
                if (!uncaught.isEmpty()) {
                    result.addUncaughtExceptions(uncaught);
                    solver.getCallGraph()
                            .callersOf(csMethod)
                            .forEach(csCallSite -> {
                                Stmt invoke = csCallSite.getCallSite();
                                CSMethod caller = csCallSite.getContainer();
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
     * @param currentStmt the statements that throws exceptions
     * @param newExceptions the new-found exceptions thrown by currentStmt
     * @param csMethod the csMethod containing currentStmt
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
        solver.getCallGraph()
                .reachableMethods()
                .forEach(csMethod -> {
                    JMethod method = csMethod.getMethod();
                    MethodThrowResult result = throwResult.getOrCreateResult(method);
                    csMethod.getThrowResult().ifPresent(result::addCSMethodThrowResult);
                });
    }
}
