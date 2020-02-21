package sa.pta.analysis.solver;

import sa.callgraph.CallGraph;
import sa.callgraph.CallKind;
import sa.callgraph.Edge;
import sa.pta.analysis.ProgramManager;
import sa.pta.analysis.context.Context;
import sa.pta.analysis.context.ContextSelector;
import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.analysis.data.CSObj;
import sa.pta.analysis.data.CSVariable;
import sa.pta.analysis.data.DataManager;
import sa.pta.analysis.data.InstanceField;
import sa.pta.analysis.data.Pointer;
import sa.pta.analysis.heap.HeapModel;
import sa.pta.element.CallSite;
import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Type;
import sa.pta.element.Variable;
import sa.pta.set.PointsToSet;
import sa.pta.set.PointsToSetFactory;
import sa.pta.statement.Allocation;
import sa.pta.statement.Assign;
import sa.pta.statement.Call;
import sa.pta.statement.InstanceLoad;
import sa.pta.statement.InstanceStore;
import sa.pta.statement.Statement;
import sa.util.AnalysisException;

import java.util.List;
import java.util.stream.Stream;

public class PointerAnalysisImpl implements PointerAnalysis {

    private ProgramManager programManager;

    private DataManager dataManager;

    private OnFlyCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private HeapModel heapModel;

    private ContextSelector contextSelector;

    private PointsToSetFactory setFactory;

    private WorkList workList;

    @Override
    public HeapModel getHeapModel() {
        return heapModel;
    }

    @Override
    public ContextSelector getContextSelector() {
        return contextSelector;
    }

    @Override
    public PointsToSetFactory getPointsToSetFactory() {
        return setFactory;
    }

    @Override
    public ProgramManager getProgramManager() {
        return null;
    }

    @Override
    public CallGraph<CSCallSite, CSMethod> getCallGraph() {
        return callGraph;
    }

    public void solve() {
        initialize();
        propagate();
    }

    private void initialize() {
        callGraph = new OnFlyCallGraph(dataManager);
        pointerFlowGraph = new PointerFlowGraph();
        workList = new WorkList();
        for (Method entry : programManager.getEntryMethods()) {
            CSMethod csMethod = dataManager.getCSMethod(
                    contextSelector.getDefaultContext(), entry);
            callGraph.addEntryMethod(csMethod);
            processNewMethod(csMethod);
        }
    }

    private void propagate() {
        while (workList.hasPointerEntries()) {
            WorkList.Entry entry = workList.pollPointerEntry();
            Pointer p = entry.pointer;
            PointsToSet pts = entry.pointsToSet;
            propagateSet(p, pts);
            if (p instanceof CSVariable) {
                CSVariable v = (CSVariable) p;
                processInstanceStore(v, pts);
                processInstanceLoad(v, pts);
                processCall(v, pts);
            }
            processCallEdges();
        }
    }

    private void propagateSet(Pointer pointer, PointsToSet pointsToSet) {
        PointsToSet diff = setFactory.makePointsToSet();
        for (CSObj obj : pointsToSet) {
            if (pointer.getPointsToSet().addObject(obj)) {
                diff.addObject(obj);
            }
        }
        if (!diff.isEmpty()) {
            for (PointerFlowEdge edge : pointerFlowGraph.getOutEdgesOf(pointer)) {
                Pointer to = edge.getTo();
                workList.addPointerEntry(to, diff);
            }
        }
    }

    private void processInstanceStore(CSVariable baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Variable var = baseVar.getVariable();
        for (InstanceStore store : var.getStores()) {
            CSVariable from = dataManager.getCSVariable(context, store.getFrom());
            for (CSObj baseObj : pts) {
                InstanceField instField = dataManager.getInstanceField(
                        baseObj, store.getField());
                if (pointerFlowGraph.addEdge(from, instField,
                        PointerFlowEdge.Kind.INSTANCE_STORE)) {
                    workList.addPointerEntry(instField, from.getPointsToSet());
                }
            }
        }
    }

    private void processInstanceLoad(CSVariable baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Variable var = baseVar.getVariable();
        for (InstanceLoad load : var.getLoads()) {
            CSVariable to = dataManager.getCSVariable(context, load.getTo());
            for (CSObj baseObj : pts) {
                InstanceField instField = dataManager.getInstanceField(
                        baseObj, load.getField());
                if (pointerFlowGraph.addEdge(instField, to,
                        PointerFlowEdge.Kind.INSTANCE_LOAD)) {
                    workList.addPointerEntry(to, instField.getPointsToSet());
                }
            }
        }
    }

    private void processCall(CSVariable recv, PointsToSet pts) {
        Context context = recv.getContext();
        Variable var = recv.getVariable();
        for (CallSite callSite : var.getCallSites()) {
            for (CSObj recvObj : pts) {
                // resolve callee
                Type type = recvObj.getObject().getType();
                Method callee;
                CallKind callKind;
                if (callSite.isVirtual()) {
                    callee = programManager.resolveVirtualCall(type, callSite.getMethod());
                    callKind = CallKind.VIRTUAL;
                } else if (callSite.isSpecial()){
                    callee = programManager.resolveSpecialCall(
                            callSite, callSite.getContainerMethod());
                    callKind = CallKind.SPECIAL;
                } else {
                    throw new AnalysisException("Unknown CallSite: " + callSite);
                }
                // select context
                CSCallSite csCallSite = dataManager.getCSCallSite(context, callSite);
                Context calleeContext = contextSelector.selectContext(
                        csCallSite, recvObj, callee);
                // build call edges
                CSMethod csCallee = dataManager.getCSMethod(calleeContext, callee);
                workList.addEdge(new Edge<>(callKind, csCallSite, csCallee));
                // pass receiver objects
                CSVariable thisVar = dataManager.getCSVariable(
                        calleeContext, callee.getThis());
                workList.addPointerEntry(thisVar,
                        setFactory.makePointsToSet(recvObj));
            }
        }
    }

    private void processNewMethod(CSMethod csMethod) {
        // mark csMethod as reachable
        if (callGraph.addNewMethod(csMethod)) {
            addNewMethodToPFG(csMethod);
            processStaticCalls(csMethod);
            processAllocations(csMethod);
        }
    }

    /**
     * Add new method to pointer flow graph.
     */
    private void addNewMethodToPFG(CSMethod csMethod) {
        Context context = csMethod.getContext();
        Method method = csMethod.getMethod();
        // add this, parameters and return variables to PFG
        if (!method.isStatic()) {
            addVarToPFG(context, method.getThis());
        }
        Stream.concat(method.getParameters().stream(),
                method.getReturnVariables().stream())
                .forEach(var -> addVarToPFG(context, var));
        // add variables of statements to PFG
        for (Statement stmt : method.getStatements()) {
            switch (stmt.getKind()) {
                case ALLOCATION:
                    Allocation alloc = (Allocation) stmt;
                    addVarToPFG(context, alloc.getVar());
                    break;
                case ASSIGN: {
                    Assign assign = (Assign) stmt;
                    CSVariable from = dataManager.getCSVariable(context, assign.getFrom());
                    CSVariable to = dataManager.getCSVariable(context, assign.getTo());
                    pointerFlowGraph.addEdge(from, to, PointerFlowEdge.Kind.LOCAL_ASSIGN);
                    break;
                }
                case INSTANCE_LOAD: {
                    InstanceLoad instLoad = (InstanceLoad) stmt;
                    addVarToPFG(context, instLoad.getBase());
                    addVarToPFG(context, instLoad.getTo());
                    break;
                }
                case INSTANCE_STORE: {
                    InstanceStore instStore = (InstanceStore) stmt;
                    addVarToPFG(context, instStore.getBase());
                    addVarToPFG(context, instStore.getFrom());
                    break;
                }
                case CALL: {
                    Call call = (Call) stmt;
                    CallSite callSite = call.getCallSite();
                    if (!callSite.isStatic()) {
                        addVarToPFG(context, callSite.getReceiver());
                    }
                    callSite.getArguments()
                            .forEach(arg -> addVarToPFG(context, arg));
                    if (callSite.getLHS() != null) {
                        addVarToPFG(context, callSite.getLHS());
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void addVarToPFG(Context context, Variable var) {
        pointerFlowGraph.addNewPointer(dataManager.getCSVariable(context, var));
    }

    /**
     * Process static calls in given context-sensitive method.
     */
    private void processStaticCalls(CSMethod csMethod) {
        Context context = csMethod.getContext();
        Method method = csMethod.getMethod();
        for (Statement stmt : method.getStatements()) {
            if (stmt.getKind() == Statement.Kind.CALL) {
                CallSite callSite = (CallSite) stmt;
                if (callSite.isStatic()) {
                    Method callee = callSite.getMethod();
                    CSCallSite csCallSite = dataManager.getCSCallSite(context, callSite);
                    Context calleeCtx = contextSelector.selectContext(csCallSite, callee);
                    CSMethod csCallee = dataManager.getCSMethod(calleeCtx, callee);
                    Edge<CSCallSite, CSMethod> edge =
                            new Edge<>(CallKind.STATIC, csCallSite, csCallee);
                    workList.addEdge(edge);
                }
            }
        }
    }

    /**
     * Process allocation statements in given context-sensitive method.
     */
    private void processAllocations(CSMethod csMethod) {
        Context context = csMethod.getContext();
        Method method = csMethod.getMethod();
        for (Statement stmt : method.getStatements()) {
            if (stmt.getKind() == Statement.Kind.ALLOCATION) {
                Allocation alloc = (Allocation) stmt;
                // obtain context-sensitive heap object
                Object allocSite = alloc.getAllocationSite();
                Obj obj = heapModel.getObj(allocSite);
                Context heapContext = contextSelector.selectHeapContext(csMethod, obj);
                CSObj csObj = dataManager.getCSObj(heapContext, obj);
                // obtain lhs variable
                CSVariable lhs = dataManager.getCSVariable(context, alloc.getVar());
                workList.addPointerEntry(lhs, setFactory.makePointsToSet(csObj));
            }
        }
    }

    /**
     * Process the call edges in work list.
     */
    private void processCallEdges() {
        while (workList.hasEdges()) {
            Edge<CSCallSite, CSMethod> edge = workList.pollEdge();
            if (!callGraph.containsEdge(edge)) {
                callGraph.addEdge(edge);
                CSMethod csCallee = edge.getCallee();
                processNewMethod(csCallee);
                Context callerCtx = edge.getCallSite().getContext();
                CallSite callSite = edge.getCallSite().getCallSite();
                Context calleeCtx = csCallee.getContext();
                Method callee = csCallee.getMethod();
                // pass arguments to parameters
                List<Variable> args = callSite.getArguments();
                List<Variable> params = callee.getParameters();
                for (int i = 0; i < args.size(); ++i) {
                    CSVariable arg = dataManager.getCSVariable(callerCtx, args.get(i));
                    CSVariable param = dataManager.getCSVariable(calleeCtx, params.get(i));
                    pointerFlowGraph.addEdge(arg, param,
                            PointerFlowEdge.Kind.PARAMETER_PASSING);
                    workList.addPointerEntry(param, arg.getPointsToSet());
                }
                // pass results to LHS variable
                if (callSite.getLHS() != null) {
                    CSVariable lhs = dataManager.getCSVariable(callerCtx, callSite.getLHS());
                    for (Variable ret : callee.getReturnVariables()) {
                        CSVariable csRet = dataManager.getCSVariable(calleeCtx, ret);
                        pointerFlowGraph.addEdge(csRet, lhs,
                                PointerFlowEdge.Kind.RETURN);
                        workList.addPointerEntry(lhs, csRet.getPointsToSet());
                    }
                }
            }
        }
    }
}
