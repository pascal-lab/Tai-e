package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.ContainerAccessHandler;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.ContainerConfig;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.HostKind;
import pascal.taie.analysis.pta.plugin.cutshortcut.field.*;
import pascal.taie.analysis.pta.plugin.reflection.ReflectiveCallEdge;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.Set;

import static pascal.taie.analysis.pta.plugin.cutshortcut.ReflectiveEdgeProperty.setVirtualArg;
import static pascal.taie.analysis.pta.plugin.cutshortcut.SpecialVariables.isNonRelay;
import static pascal.taie.analysis.pta.plugin.cutshortcut.container.ClassAndTypeClassifier.isHashtableType;
import static pascal.taie.analysis.pta.plugin.cutshortcut.container.ClassAndTypeClassifier.isVectorType;

public class CutShortcutSolver extends DefaultSolver {
    private ContainerConfig containerConfig;

    private FieldAccessHandler fieldAccessHandler = null;

    private ContainerAccessHandler containerAccessHandler = null;

    // cutRetuns resolved By LocalFlowHandler
    private final Set<Var> cutReturnVars = Sets.newSet();

    private final Set<StoreField> cutStoreFields = Sets.newSet();

    private final Set<JMethod> selectedMethods = Sets.newSet();

    // callsites whose callee should not be cut, handle container access whose object type is not modeled
    private final Set<CSCallSite> recoveredCallSites = Sets.newSet();

    public CutShortcutSolver(AnalysisOptions options, HeapModel heapModel,
                             ContextSelector contextSelector, CSManager csManager) {
        super(options, heapModel, contextSelector, csManager);
    }

    @Override
    public void setPlugin(Plugin plugin) {
        super.setPlugin(plugin);
        assert plugin instanceof CompositePlugin;
        CompositePlugin compositePlugin = (CompositePlugin) plugin;
        for (Plugin p : compositePlugin.getAllPlugins()) {
            if (p instanceof FieldAccessHandler fah)
                fieldAccessHandler = fah;
            else if (p instanceof ContainerAccessHandler cah)
                containerAccessHandler = cah;
        }
        assert fieldAccessHandler != null;
        assert containerAccessHandler != null;
    }

    // ---------- solver logic starts ----------
    /**
     * Initializes pointer analysis.
     */
    @Override
    protected void initialize() {
        containerConfig = ContainerConfig.config;
        super.initialize();
    }

    /**
     * Processes worklist entries until the worklist is empty.
     */
    @Override
    protected void analyze() {
        while (!workList.isEmpty()) {
            WorkList.Entry entry = workList.pollEntry();
            if (entry instanceof WorkList.PointerEntry pEntry) {
                Pointer p = pEntry.pointer();
                PointsToSet pts = pEntry.pointsToSet();
                PointsToSet diff = propagate(p, pts);
                if (!diff.isEmpty() && p instanceof CSVar v) {
                    processInstanceStore(v, diff);
                    processInstanceLoad(v, diff);
                    processArrayStore(v, diff);
                    processArrayLoad(v, diff);
                    processCall(v, diff);
                    plugin.onNewPointsToSet(v, diff);
                }
            }
            else if (entry instanceof WorkList.CallEdgeEntry eEntry)
                processCallEdge(eEntry.edge());
            else if (entry instanceof WorkList.SetStmtEntry sEntry)
                fieldAccessHandler.onNewSetStatement(sEntry.csMethod(), sEntry.setStmt());
            else if (entry instanceof WorkList.GetStmtEntry gEntry)
                fieldAccessHandler.onNewGetStatement(gEntry.csMethod(), gEntry.getStmt());
            else if (entry instanceof WorkList.HostEntry hEntry) {
                Pointer p = hEntry.pointer();
                PointsToSet diff = processHostEntry(hEntry);
                if (p instanceof CSVar csVar && !diff.isEmpty())
                    containerAccessHandler.onNewHostEntry(csVar, diff, hEntry.kind());
            }
        }
        plugin.onFinish();
    }

    public boolean addRecoveredCallSite(CSCallSite csCallSite) {
        return recoveredCallSites.add(csCallSite);
    }

    public boolean isRecoveredCallSite(CSCallSite csCallSite) {
        return recoveredCallSites.contains(csCallSite);
    }

    String[] stopSigns = new String[]{"iterator(", "entrySet()", "keySet()", "values()", "Entry(", "Iterator("};

    /*
     * @param source:  PFG edge s --> v, if source inside Transfer method, do not propagate ptsH
     */
    public boolean needPropagateHost(PointerFlowEdge edge) {
        // Todo: Here return var of Map.values() --> r is treated as Local_Assign
        if (edge.kind() == FlowKind.RETURN) {
            Var sourceVar = ((CSVar) edge.source()).getVar();
            JClass container = sourceVar.getMethod().getDeclaringClass();
            String methodString = sourceVar.getMethod().toString();
            // container, entryset type
            if (containerConfig.isHostClass(container)) {
                // source variable is inside function: iterator, entryset, keyset, values. do not propagate to host
                for (String stopSign: stopSigns) {
                    if (methodString.contains(stopSign))
                        return false;
                }
                // HashTable.elements() return Enumeration of HashTable.values(), .keys() return Enumeration of keys
                if (isHashtableType(container.getType()) && (methodString.contains("elements()") || methodString.contains("keys()")))
                    return false;
                // vector.elements() return Enumeration of Vector.elements
                return !isVectorType(container.getType()) || !methodString.contains("elements()");
            }
            return true;
        }
        return true;
    }

    /**
     * @return true if the type of given expression is concerned in
     * pointer analysis, otherwise false.
     */
    public static boolean isConcerned(Exp exp) {
        Type type = exp.getType();
        return type instanceof ReferenceType && !(type instanceof NullType);
    }

    public void addCutStoreField(StoreField set) { // 需要跳过的StoreField，位于最内层的set方法
        cutStoreFields.add(set);
    }

    private PointsToSet processHostEntry(WorkList.HostEntry entry) {
        Pointer pointer = entry.pointer();
        PointsToSet hostSet = entry.hostSet();
        HostKind kind = entry.kind();
        PointsToSet diff = containerAccessHandler.getHostListOf(pointer, kind).addAllDiff(hostSet);
        if (!diff.isEmpty()) {
            pointerFlowGraph.getOutEdgesOf(pointer).forEach(edge -> {
                if (needPropagateHost(edge)) {
                    Pointer target = edge.target();
                    workList.addHostEntry(target, kind, diff);
                }
            });
        }
        return diff;
    }

    private void processInstanceStore(CSVar baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Var var = baseVar.getVar();
        for (StoreField store : var.getStoreFields()) {
            // skip cutStores
            if (cutStoreFields.contains(store))
                continue;
            Var fromVar = store.getRValue();
            if (propTypes.isAllowed(fromVar)) {
                CSVar from = getCSManager().getCSVar(context, fromVar);
                pts.forEach(baseObj -> {
                    JField field = store.getFieldRef().resolve();
                    InstanceField instField = getCSManager().getInstanceField(baseObj, field);
                    addPFGEdge(from, instField, FlowKind.INSTANCE_STORE);
                });
            }
        }
    }

    private void processInstanceLoad(CSVar baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Var var = baseVar.getVar();
        for (LoadField load : var.getLoadFields()) {
            Var toVar = load.getLValue();
            JField field = load.getFieldRef().resolveNullable();
            if (propTypes.isAllowed(toVar) && field != null) {
                CSVar to = getCSManager().getCSVar(context, toVar);
                pts.forEach(baseObj -> {
                    InstanceField instField = getCSManager().getInstanceField(
                            baseObj, field);
                    addPFGEdge(instField, to, //toVar.getType(),
                            isNonRelay(load) ? FlowKind.NON_RELAY_GET : FlowKind.INSTANCE_LOAD);
                });
            }
        }
    }

    public void processCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (callGraph.addEdge(edge)) {
            if (edge instanceof ReflectiveCallEdge reflEdge)
                setVirtualArg(reflEdge);
            // process new call edge
            CSMethod csCallee = edge.getCallee();
            addCSMethod(csCallee);
            if (edge.getKind() != CallKind.OTHER && !isIgnored(csCallee.getMethod())) {
                CSCallSite csCallSite = edge.getCallSite();
                Context callerCtx = edge.getCallSite().getContext();
                Invoke callSite = edge.getCallSite().getCallSite();
                Context calleeCtx = csCallee.getContext();
                JMethod callee = csCallee.getMethod();
                InvokeExp invokeExp = callSite.getInvokeExp();
                // pass arguments to parameters
                for (int i = 0; i < invokeExp.getArgCount(); ++i) {
                    Var arg = invokeExp.getArg(i);
                    if (propTypes.isAllowed(arg)) {
                        Var param = callee.getIR().getParam(i);
                        CSVar argVar = getCSManager().getCSVar(callerCtx, arg);
                        CSVar paramVar = getCSManager().getCSVar(calleeCtx, param);
                        addPFGEdge(argVar, paramVar, FlowKind.PARAMETER_PASSING);
                    }
                }
                // pass results to LHS variable
                Var lhs = callSite.getResult();
                if (!ContainerAccessHandler.CutReturnEdge(lhs, callee) || recoveredCallSites.contains(csCallSite)) {
                    if (lhs != null && propTypes.isAllowed(lhs)) {
                        CSVar csLHS = getCSManager().getCSVar(callerCtx, lhs);
                        for (Var ret : callee.getIR().getReturnVars()) {
                            if (propTypes.isAllowed(ret) && !cutReturnVars.contains(ret)) {
                                CSVar csRet = getCSManager().getCSVar(calleeCtx, ret);
                                addPFGEdge(csRet, csLHS, FlowKind.RETURN);
                            }
                        }
                    }
                }
            }
            plugin.onNewCallEdge(edge);
        }
    }

    public void addPFGEdge(Pointer source, Pointer target, FlowKind kind, Set<Transfer> transfers) {
        PointerFlowEdge edge = new PointerFlowEdge(kind, source, target);
        if (pointerFlowGraph.addEdge(edge) != null) {
            PointsToSet sourceSet = getPointsToSetOf(source);
            PointsToSet targetSet = makePointsToSet();
            transfers.forEach(transfer -> {
                if (edge.addTransfer(transfer)) {
                    PointsToSet transferSet = transfer.apply(edge, sourceSet);
                    targetSet.addAll(transferSet);
                }
            });
            if (!targetSet.isEmpty())
                addPointsTo(target, targetSet);
            plugin.onNewPFGEdge(edge);
        }
    }

    @Override
    public void addPFGEdge(PointerFlowEdge edge, Transfer transfer) {
        edge = pointerFlowGraph.addEdge(edge);
        if (edge != null) {
            if (edge.addTransfer(transfer)) {
                PointsToSet targetSet = transfer.apply(edge, getPointsToSetOf(edge.source()));
                if (!targetSet.isEmpty())
                    addPointsTo(edge.target(), targetSet);
            }
            plugin.onNewPFGEdge(edge);
        }
    }

    public void addSetStmtEntry(CSMethod csMethod, SetStatement setStmt) {
        workList.addSetStmtEntry(csMethod, setStmt);
    }

    public void addGetStmtEntry(CSMethod csMethod, GetStatement getStmt) {
        workList.addGetStmtEntry(csMethod, getStmt);
    }

    public void addHostEntry(Pointer pointer, HostKind kind, PointsToSet hostSet) {
        if (!hostSet.isEmpty())
            workList.addHostEntry(pointer, kind, hostSet);
    }

    public void addCutReturnVar(Var ret) {
        cutReturnVars.add(ret);
    }

    public void addSelectedMethod(JMethod method) { selectedMethods.add(method); }

    public Set<JMethod> getInvolvedMethods() {
        return selectedMethods;
    }
}
