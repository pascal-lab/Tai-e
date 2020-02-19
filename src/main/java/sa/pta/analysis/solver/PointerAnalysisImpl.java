package sa.pta.analysis.solver;

import sa.callgraph.CallGraph;
import sa.pta.analysis.ProgramManager;
import sa.pta.analysis.context.Context;
import sa.pta.analysis.context.ContextSelector;
import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.analysis.data.CSObj;
import sa.pta.analysis.data.CSVariable;
import sa.pta.analysis.data.ElementManager;
import sa.pta.analysis.data.Pointer;
import sa.pta.analysis.heap.HeapModel;
import sa.pta.element.CallSite;
import sa.pta.element.Method;
import sa.pta.element.Obj;
import sa.pta.element.Variable;
import sa.pta.set.PointsToSetFactory;
import sa.pta.statement.Allocation;
import sa.pta.statement.Assign;
import sa.pta.statement.Call;
import sa.pta.statement.InstanceLoad;
import sa.pta.statement.InstanceStore;
import sa.pta.statement.Statement;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;

public class PointerAnalysisImpl implements PointerAnalysis {

    private ProgramManager programManager;

    private ElementManager elementManager;

    private OnFlyCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private HeapModel heapModel;

    private ContextSelector contextSelector;

    private PointsToSetFactory setFactory;

    private Queue<Pointer> workList;

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

    }

    private void initialize() {
        callGraph = new OnFlyCallGraph(elementManager);
        pointerFlowGraph = new PointerFlowGraph(setFactory);
        workList = new LinkedList<>();
        programManager.getEntryMethods()
                .stream()
                .map(m -> elementManager
                        .getCSMethod(contextSelector.getDefaultContext(), m))
                .forEach(m -> {
                    callGraph.addEntryMethod(m);
                    processNewMethod(m);
                });
    }

    private void processNewMethod(CSMethod csmethod) {
        // build pointer flow graph for the method
        addNewMethodToPFG(csmethod);
        // process static calls in this method

        // process allocation statements of this method
        processAllocation(csmethod);
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
                    CSVariable from = elementManager.getCSVariable(context, assign.getFrom());
                    CSVariable to = elementManager.getCSVariable(context, assign.getTo());
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
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void addVarToPFG(Context context, Variable var) {
        pointerFlowGraph.addNewPointer(
                elementManager.getCSVariable(context, var));
    }

    private void processAllocation(CSMethod csMethod) {
        Context context = csMethod.getContext();
        Method method = csMethod.getMethod();
        for (Statement stmt : method.getStatements()) {
            if (stmt.getKind() == Statement.Kind.ALLOCATION) {
                Allocation alloc = (Allocation) stmt;
                // obtain context-sensitive heap object
                Object allocSite = alloc.getAllocationSite();
                Obj obj = heapModel.getObj(allocSite);
                Context heapContext = contextSelector.selectHeapContext(csMethod, obj);
                CSObj csObj = elementManager.getCSObj(heapContext, obj);
                // obtain lhs variable
                CSVariable lhs = elementManager.getCSVariable(context, alloc.getVar());
                lhs.getPointsToSet().addObject(csObj);
                workList.add(lhs);
            }
        }
    }
}
