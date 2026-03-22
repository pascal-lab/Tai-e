package pascal.taie.analysis.pta.plugin.cutshortcut.field;

import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.solver.CutShortcutSolver;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.ContainerAccessHandler;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.*;
import java.util.stream.Collectors;

import static pascal.taie.analysis.pta.core.solver.CutShortcutSolver.isConcerned;
import static pascal.taie.analysis.pta.plugin.cutshortcut.SpecialVariables.*;
import static pascal.taie.analysis.pta.plugin.cutshortcut.field.ParameterIndex.THISINDEX;
import static pascal.taie.analysis.pta.plugin.cutshortcut.field.ParameterIndex.getRealParameterIndex;

public class FieldAccessHandler implements Plugin {
    private final MultiMap<CSMethod, SetStatement> setStatements = Maps.newMultiMap();

    private final MultiMap<CSMethod, GetStatement> getStatements = Maps.newMultiMap();

    private final Set<Var> cutReturnVars = Sets.newSet();

    private final Map<Var, List<AbstractLoadField>> abstractLoadFields = Maps.newMap();

    private final Map<Var, List<AbstractStoreField>> abstractStoreFields = Maps.newMap();

    private CutShortcutSolver solver;

    private TypeSystem typeSystem;

    private CSManager csManager;

    private CallGraph<CSCallSite, CSMethod> callGraph;

    private Context emptyContext;

    @Override
    public void setSolver(Solver solver) {
        if (solver instanceof CutShortcutSolver cutShortcutSolver) {
            this.solver = cutShortcutSolver;
            typeSystem = World.get().getTypeSystem();
            callGraph = solver.getCallGraph();
            csManager = solver.getCSManager();
            emptyContext = solver.getContextSelector().getEmptyContext();
        }
        else
            throw new AnalysisException("Invalid Solver to " + getClass());
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        processAbstractInstanceLoad(csVar, pts);
        processAbstractInstanceStore(csVar, pts);
    }

    // process abstract instance load: <c, lhs = base.f>, \forall <c', o> \in pts(<c, base>), add <c', o.f> --> <c, lhs>
    private void processAbstractInstanceLoad(CSVar csVar, PointsToSet pts) {
        Context baseContext = csVar.getContext(); // c
        Var base = csVar.getVar(); // lhs
        getAbstractLoadFields(base).forEach(load -> {
            Var lhs = load.getLValue();
            JField field = load.getFieldRef().resolve();
            if (isConcerned(lhs) && field != null) {
                CSVar csLHS = csManager.getCSVar(baseContext, lhs);
                pts.forEach(baseObj -> {
                    if (typeSystem.isSubtype(field.getDeclaringClass().getType(), baseObj.getObject().getType())) {
                        InstanceField instField = csManager.getInstanceField(baseObj, field);
                        solver.addPFGEdge(instField, csLHS, // lhs.getType(),
                                load.isNonRelay() ? FlowKind.NON_RELAY_GET : FlowKind.GET);
                    }
                });
            }
        });
    }

    // process abstract store: <c, base.f = rhs>, \forall <c', o> \in pts(<c, base>), add <c, rhs> --> <c', o.f>
    private void processAbstractInstanceStore(CSVar csVar, PointsToSet pts) {
        Context baseContext = csVar.getContext();
        Var base = csVar.getVar();
        getAbstractStoreFields(base).forEach(store -> {
            Var rhs = store.getRValue();
            JField field = store.getFieldRef().resolve();
            if (isConcerned(rhs) && field != null) {
                CSVar csRHS = csManager.getCSVar(baseContext, rhs);
                pts.forEach(baseObj -> {
                    if (typeSystem.isSubtype(field.getDeclaringClass().getType(), baseObj.getObject().getType())) {
                        InstanceField instField = csManager.getInstanceField(baseObj, field);
                        solver.addPFGEdge(csRHS, instField, FlowKind.SET, field.getType());
                    }
                });
            }
        });
    }

    // [CutStore], [CutPropLoad]
    @Override
    public void onNewMethod(JMethod method) {
        if (method.isAbstract())
            return;
        IR methodIR = method.getIR();
        methodIR.forEach(stmt -> {
            if (stmt.getDef().isPresent() && stmt.getDef().get() instanceof Var def)
                setDefined(def);
        });
        if (methodIR.getThis() != null)
            setParameterIndex(methodIR.getThis(), THISINDEX);
        List<Var> params = methodIR.getParams();
        for (int i = 0; i < params.size(); i++)
            setParameterIndex(params.get(i), getRealParameterIndex(i));
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        if (method.isAbstract())
            return;
        JClass declaringClass = method.getDeclaringClass();
        method.getIR().forEach(stmt -> {
            if (!declaringClass.getName().equals("java.awt.Component")
                    && !declaringClass.getName().equals("javax.swing.JComponent")
                    && stmt instanceof LoadField load
                    && load.getFieldAccess() instanceof InstanceFieldAccess fieldAccess) {
                // m_ret(x) = m_pi.f;
                Var x = load.getLValue(), y = fieldAccess.getBase();
                if (isConcerned(x)) {
                    int retIndex = ParameterIndex.GetReturnVariableIndex(x);
                    ParameterIndex baseIndex = getParameterIndex(y);
                    if (retIndex >= 0 && baseIndex != null && !isDefined(y)) {
                        // the load inst should be relayed
                        disableRelay(load);
                        addCutReturnVar(csMethod, x);
                        GetStatement getStatement = new GetStatement(retIndex, baseIndex, load.getFieldRef());
                        solver.addGetStmtEntry(csMethod, getStatement);
                    }
                }
            }
            // not entry method, each entry method is associated with empty context, so this condition can be used to filter non-entry method
            else if (!callGraph.entryMethods().collect(Collectors.toSet()).contains(csManager.getCSMethod(emptyContext, method))
                    && stmt instanceof StoreField store
                    && store.getFieldAccess() instanceof InstanceFieldAccess fieldAccess) {
                // m_pi.f = m_pj;
                Var x = fieldAccess.getBase(), y = store.getRValue();
                if (isConcerned(y)) {
                    ParameterIndex baseIndex = getParameterIndex(x), rhsIndex = getParameterIndex(y);
                    if (baseIndex != null && rhsIndex != null && !isDefined(x) && !isDefined(y)) {
                        solver.addCutStoreField(store);
                        SetStatement setStatement = new SetStatement(baseIndex, store.getFieldRef(), rhsIndex);
                        solver.addSetStmtEntry(csMethod, setStatement);
                    }
                }
            }
        });
    }

    /*
     * work when l first appears in <c, m>
     * @param csMethod: <c, m>,
     * @param setStmt: l: m_pi.f = m_pj \in m
     */
    public void onNewSetStatement(CSMethod csMethod, SetStatement setStmt) {
        if (addSetStatement(csMethod, setStmt))
            callGraph.edgesInTo(csMethod).forEach(
                edge -> processSetStatementOnCallEdge(edge, setStmt));
    }

    /*
     * work when l first appears in <c, m>
     * @param csMethod: <c, m>,
     * @param getStmt: l: m_ret = m_pi.f \in m
     */
    public void onNewGetStatement(CSMethod csMethod, GetStatement getStmt) {
        // add deleted return vars (only do it when a new set statement is found)
        if (addGetStatement(csMethod, getStmt))
            callGraph.edgesInTo(csMethod).forEach(
                edge -> processGetStatementOnCallEdge(edge, getStmt));
    }

    /*
     * [PropStore], generate temp store: <c, m_pu.f = m_pv> or concrete abstract store: <c, l_ai.f = l_aj>
     * @param edge: <c, l: v.k(l_ai, ...)> --> <c^t, m>,
     * @param setStmt: m_pi.f = m_pj \in m
     */
    private void processSetStatementOnCallEdge(Edge<CSCallSite, CSMethod> edge, SetStatement setStmt) {
        Var base = ParameterIndex.getCorrespondingArgument(edge, setStmt.baseIndex()), // l_ai
            rhs = ParameterIndex.getCorrespondingArgument(edge, setStmt.rhsIndex()); // l_aj
        if (base != null && rhs != null && isConcerned(rhs)) {
            CSMethod csCaller = edge.getCallSite().getContainer(); // <c, m_caller>
            ParameterIndex baseIndexAtCaller = getParameterIndex(base),  // l_ai \is m_pu of m_caller
                    rhsIndexAtCaller = getParameterIndex(rhs); // l_aj \is m_pv of m_caller
            if (baseIndexAtCaller != null && rhsIndexAtCaller != null && !isDefined(base) && !isDefined(rhs)) {
                solver.addSelectedMethod(edge.getCallee().getMethod());
                // setStmt: m_pu.f = m_pv
                solver.addSetStmtEntry(csCaller, new SetStatement(baseIndexAtCaller, setStmt.fieldRef(), rhsIndexAtCaller));
            }
            // top-level temp store, generate a new store field in current context
            else
                processNewAbstractStoreField(edge.getCallSite().getContext(), base, setStmt.fieldRef(), rhs);
        }
    }

    /**
     * [CutPropLoad], add new return var m_ret, generate temp load:
     * @param edge: <c, l: m_caller_ret = v.k(..., l_ai, ...)> --> <c^t, m>
     * @param getStmt: m_ret = m_pi.f \in m
     */
    private void processGetStatementOnCallEdge(Edge<CSCallSite, CSMethod> edge, GetStatement getStmt) {
        // ToDo: consider context-sensitive setting
        Invoke callSite = edge.getCallSite().getCallSite(); // l
        JMethod callee = edge.getCallee().getMethod(); // m
        Var base = ParameterIndex.getCorrespondingArgument(edge, getStmt.baseIndex()), // l_ai
                lhs = callSite.getLValue(); // m_caller_ret
        if (!ContainerAccessHandler.CutReturnEdge(lhs, callee)) {
            if (base != null && lhs != null && isConcerned(lhs)) {
                CSMethod csCaller = callGraph.getContainerOf(edge.getCallSite()); // <c, m_caller>
                int lhsIndexAtCaller = ParameterIndex.GetReturnVariableIndex(lhs); // index of m_caller_ret
                ParameterIndex baseIndexAtCaller = getParameterIndex(base); // index of l_ai
                solver.addSelectedMethod(edge.getCallee().getMethod());
                // m_caller_ret is return value of m_caller, l_ai is parameter/this of m_caller, [PropLoad] to caller of m_caller
                if (lhsIndexAtCaller != -1 && baseIndexAtCaller != null) {
                    addCutReturnVar(csCaller, lhs); // 每一层GetStatement的retVar都需要删除
                    solver.addGetStmtEntry(csCaller, new GetStatement(lhsIndexAtCaller, baseIndexAtCaller, getStmt.fieldRef()));
                    // new generated PFG edges are non-relay
                    processNewAbstractLoadField(edge.getCallSite().getContext(), lhs, base, getStmt.fieldRef(), false);
                }
                // top-level temp load, generate a new load: <c, lhs = base.f>, new generated PFG edge is terminal(relayed)
                else
                    processNewAbstractLoadField(edge.getCallSite().getContext(), lhs, base, getStmt.fieldRef(), true);
            }
        }
    }

    // [ShortcutStore]: in context c, generate a new store field: <c, base.f = rhs>
    private void processNewAbstractStoreField(Context context, Var base, FieldRef fieldRef, Var rhs) {
        CSVar csBase = csManager.getCSVar(context, base),  // <c, base>
              csRHS = csManager.getCSVar(context, rhs); // <c, rhs>
        JField field = fieldRef.resolve();
        AbstractStoreField storeField = new AbstractStoreField(new InstanceFieldAccess(fieldRef, base), rhs);
        getAbstractStoreFields(base).add(storeField);
        // \forall <c', o> \in pts(<c, base>), add <c, rhs> --> <c', o.f>
        solver.getPointsToSetOf(csBase).forEach(csObj -> {
            if (typeSystem.isSubtype(field.getDeclaringClass().getType(), csObj.getObject().getType()))
                solver.addPFGEdge(csRHS, csManager.getInstanceField(csObj, field), FlowKind.SET, field.getType());
        });
    }

    // [ShortcutLoad]: in context c, generate a new abstract load field: <c, lhs = base.f>, terminate determines whether it is lhs = base.f terminal PFG edge
    private void processNewAbstractLoadField(Context context, Var lhs, Var base, FieldRef fieldRef, boolean terminate) {
        CSVar csBase = csManager.getCSVar(context, base), // <c, base>
                csLHS = csManager.getCSVar(context, lhs); // <c, lhs>
        JField field = fieldRef.resolve();
        AbstractLoadField loadField = new AbstractLoadField(lhs, new InstanceFieldAccess(fieldRef, base), terminate);
        getAbstractLoadFields(base).add(loadField);
        // \forall <c', o> \in pts(<c, base>), add PFG edge: <c', o.f> --> <c, lhs>
        solver.getPointsToSetOf(csBase).forEach(csObj -> {
            if (typeSystem.isSubtype(field.getDeclaringClass().getType(), csObj.getObject().getType()))
                solver.addPFGEdge(csManager.getInstanceField(csObj, field), csLHS, // lhs.getType(),
                        terminate ? FlowKind.GET : FlowKind.NON_RELAY_GET);
        });
    }

    // [RelayEdge]: <c, l: r = v.k(...)> --> <c^t, m>
    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Invoke callSite = edge.getCallSite().getCallSite(); // l
        processSetStatementOnNewCallEdge(edge);
        Var lhs = callSite.getLValue(); // r
        if (lhs != null && isConcerned(lhs)) {
            processGetStatementOnNewCallEdge(edge);
            CSMethod csCallee = edge.getCallee(); // <c^t, m>
            JMethod callee = csCallee.getMethod(); // m
            CSVar csLHS = csManager.getCSVar(edge.getCallSite().getContext(), lhs); // <c, r>
            if (!ContainerAccessHandler.CutReturnEdge(lhs, callee)) {
                for (Var ret: callee.getIR().getReturnVars()) {
                    if (cutReturnVars.contains(ret)) {
                        CSVar csRet = csManager.getCSVar(csCallee.getContext(), ret); // <c^t, m_ret>
                        csRet.getInEdges().forEach(inEdge -> { // \forall <c', v> --> <c^t, m_ret>
                            // transfer, add <c', v> --> <c, r>, inEdge.terminate = True
                            if (inEdge.kind() != FlowKind.NON_RELAY_GET)
                                solver.addPFGEdge(inEdge.source(), csLHS, inEdge.kind(), inEdge.getTransfers());
                        });
                    }
                }
            }
        }
    }

    // [CutPropLoad]: <c, l: r = v.k(...)> --> <c^t, m>,
    private void processSetStatementOnNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        // when a new call edge found, process callSite with previous found SetStatement in the callee
        getSetStatementsOf(edge.getCallee()).forEach(setStmt ->
                processSetStatementOnCallEdge(edge, setStmt));
    }

    // <c, l: r = v.k(...)> --> <c^t, m>
    private void processGetStatementOnNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        getGetStatementsOf(edge.getCallee()).forEach(getStmt ->
                processGetStatementOnCallEdge(edge, getStmt));
    }

    /*
     * [RelayEdge] rules, a relay PFG edge: <c'', n> --> <c, m_ret> should be converted to <c'', n> --> <c, r>
     * @param edge: <c'', n> --> <c, m_ret>, edge is not terminal PFG
     */
    @Override
    public void onNewPFGEdge(PointerFlowEdge edge) {
        Pointer source = edge.source(), target = edge.target();
        FlowKind kind = edge.kind();
        // m_ret \in cutReturns, <c'', n> --> <c, m_ret> is a relay PFG edge
        if (target instanceof CSVar csVar && cutReturnVars.contains(csVar.getVar()) && kind != FlowKind.NON_RELAY_GET) {
            CSMethod csMethod = csManager.getCSMethod(csVar.getContext(), csVar.getVar().getMethod()); // c
            // \forall <c'', l: r = v.k(...)> --> <c, m>, add <c'', o.f> --> <c, r>
            callGraph.getCallersOf(csMethod).forEach(csCallSite -> {
                Var lhs = csCallSite.getCallSite().getLValue(); // r
                if (lhs != null && isConcerned(lhs)) {
                    CSVar csLHS = csManager.getCSVar(csCallSite.getContext(), lhs);
                    solver.addPFGEdge(source, csLHS, kind, edge.getTransfers()); // <c'', o.f> --> <c, r>
                }
            });
        }
    }

    /*
     * [RelayEdge]: <c'', x> --> <c, m_ret> translate to <c'', x> --> <c', r>
     * @param csMethod: <c, m>,
     * @param ret: m_ret \in cutReturns
     */
    private void addCutReturnVar(CSMethod csMethod, Var ret) {
        cutReturnVars.add(ret);
        solver.addCutReturnVar(ret);
        CSVar csRet = csManager.getCSVar(csMethod.getContext(), ret); // <c, m_ret>
        csRet.getInEdges().forEach(edge -> { // <c'', x> --> <c, m_ret>
            if (edge.kind() != FlowKind.NON_RELAY_GET) {
                // <c', l: r = v.k(...)> --> <c, m>
                callGraph.getCallersOf(csMethod).forEach(csCallSite -> {
                    Var lhs = csCallSite.getCallSite().getLValue(); // r
                    if (lhs != null && isConcerned(lhs)) {
                        CSVar csLHS = csManager.getCSVar(csCallSite.getContext(), lhs); // <c', r>
                        solver.addPFGEdge(edge.source(), csLHS, edge.kind(), edge.getTransfers()); // <c'', x> --> <c', r>
                    }
                });
            }
        });
    }

    private boolean addSetStatement(CSMethod csMethod, SetStatement setStatement) {
        return setStatements.put(csMethod, setStatement);
    }

    private boolean addGetStatement(CSMethod csMethod, GetStatement getStmt) {
        return getStatements.put(csMethod, getStmt);
    }

    private Set<SetStatement> getSetStatementsOf(CSMethod csMethod) { // 得到一个方法里所有的SetStatement
        return setStatements.get(csMethod);
    }

    private Set<GetStatement> getGetStatementsOf(CSMethod csMethod) {
        return getStatements.get(csMethod);
    }


    private List<AbstractLoadField> getAbstractLoadFields(Var base) {
        return abstractLoadFields.computeIfAbsent(base, v -> new ArrayList<>());
    }

    private List<AbstractStoreField> getAbstractStoreFields(Var base) {
        return abstractStoreFields.computeIfAbsent(base, v -> new ArrayList<>());
    }
}
