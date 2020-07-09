/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.core.solver;

import bamboo.callgraph.CallGraph;
import bamboo.callgraph.CallKind;
import bamboo.callgraph.Edge;
import bamboo.pta.core.ProgramManager;
import bamboo.pta.core.context.Context;
import bamboo.pta.core.context.ContextInsensitiveSelector;
import bamboo.pta.core.context.ContextSelector;
import bamboo.pta.core.cs.ArrayIndex;
import bamboo.pta.core.cs.CSCallSite;
import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.core.cs.CSObj;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.cs.DataManager;
import bamboo.pta.core.cs.InstanceField;
import bamboo.pta.core.cs.Pointer;
import bamboo.pta.core.cs.StaticField;
import bamboo.pta.core.heap.HeapModel;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;
import bamboo.pta.element.Variable;
import bamboo.pta.set.PointsToSet;
import bamboo.pta.set.PointsToSetFactory;
import bamboo.pta.statement.Allocation;
import bamboo.pta.statement.ArrayLoad;
import bamboo.pta.statement.ArrayStore;
import bamboo.pta.statement.Assign;
import bamboo.pta.statement.AssignCast;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.InstanceLoad;
import bamboo.pta.statement.InstanceStore;
import bamboo.pta.statement.StatementVisitor;
import bamboo.pta.statement.StaticLoad;
import bamboo.pta.statement.StaticStore;
import bamboo.util.AnalysisException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private Set<Method> reachableMethods;

    private ClassInitializer classInitializer;

    @Override
    public ProgramManager getProgramManager() {
        return programManager;
    }

    @Override
    public void setProgramManager(ProgramManager programManager) {
        this.programManager = programManager;
    }

    @Override
    public DataManager getDataManager() {
        return dataManager;
    }

    @Override
    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public ContextSelector getContextSelector() {
        return contextSelector;
    }

    @Override
    public void setContextSelector(ContextSelector contextSelector) {
        this.contextSelector = contextSelector;
    }

    @Override
    public void setHeapModel(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    @Override
    public void setPointsToSetFactory(PointsToSetFactory setFactory) {
        this.setFactory = setFactory;
    }

    @Override
    public CallGraph<CSCallSite, CSMethod> getCallGraph() {
        return callGraph;
    }

    @Override
    public Stream<CSVariable> getVariables() {
        return dataManager.getCSVariables();
    }

    @Override
    public Stream<InstanceField> getInstanceFields() {
        return dataManager.getInstanceFields();
    }

    @Override
    public Stream<ArrayIndex> getArrayIndexes() {
        return dataManager.getArrayIndexes();
    }

    @Override
    public Stream<StaticField> getStaticFields() {
        return dataManager.getStaticFields();
    }

    @Override
    public boolean isContextSensitive() {
        return !(contextSelector instanceof ContextInsensitiveSelector);
    }

    /**
     * Runs pointer analysis algorithm.
     */
    @Override
    public void analyze() {
        initialize();
        solve();
    }

    /**
     * Initializes pointer analysis.
     */
    private void initialize() {
        callGraph = new OnFlyCallGraph(dataManager);
        pointerFlowGraph = new PointerFlowGraph();
        workList = new WorkList();
        reachableMethods = new HashSet<>();
        classInitializer = new ClassInitializer();

        for (Method entry : programManager.getEntryMethods()) {
            // initialize class type of entry methods
            classInitializer.initializeClass(entry.getClassType());
            CSMethod csMethod = dataManager.getCSMethod(
                    contextSelector.getDefaultContext(), entry);
            processNewCSMethod(csMethod);
            // must be called after processNewMethod()
            callGraph.addEntryMethod(csMethod);
        }
    }

    /**
     * Processes worklist entries until the worklist is empty.
     */
    private void solve() {
        while (!workList.isEmpty()) {
            while (workList.hasPointerEntries()) {
                WorkList.Entry entry = workList.pollPointerEntry();
                Pointer p = entry.pointer;
                PointsToSet pts = entry.pointsToSet;
                PointsToSet diff = propagate(p, pts);
                if (p instanceof CSVariable) {
                    CSVariable v = (CSVariable) p;
                    processInstanceStore(v, diff);
                    processInstanceLoad(v, diff);
                    processArrayStore(v, diff);
                    processArrayLoad(v, diff);
                    processCall(v, diff);
                }
            }
            while (workList.hasCallEdges()) {
                processCallEdge(workList.pollCallEdge());
            }
        }
    }

    /**
     * Propagates pointsToSet to pt(pointer) and its PFG successors,
     * returns the difference set of pointsToSet and pt(pointer).
     */
    private PointsToSet propagate(Pointer pointer, PointsToSet pointsToSet) {
        System.out.println("Propagate " + pointsToSet + " to " + pointer);
        final PointsToSet diff = setFactory.makePointsToSet();
        for (CSObj obj : pointsToSet) {
            if (pointer.getPointsToSet().addObject(obj)) {
                diff.addObject(obj);
            }
        }
        if (!diff.isEmpty()) {
            for (PointerFlowEdge edge : pointerFlowGraph.getOutEdgesOf(pointer)) {
                Pointer to = edge.getTo();
                if (edge.getType() != null) {
                    // Checks assignable objects
                    workList.addPointerEntry(to,
                            getAssignablePointsToSet(diff, edge.getType()));
                } else {
                    workList.addPointerEntry(to, diff);
                }
            }
        }
        return diff;
    }

    /**
     * Given a points-to set pts and a type t, returns the objects of pts
     * which can be assigned to t.
     */
    private PointsToSet getAssignablePointsToSet(PointsToSet pts, Type type) {
        PointsToSet result = setFactory.makePointsToSet();
        pts.stream()
                .filter(o -> programManager.canAssign(
                        o.getObject().getType(), type))
                .forEach(result::addObject);
        return result;
    }

    /**
     * Adds an edge "from -> to" to the PFG.
     */
    private void addPFGEdge(Pointer from, Pointer to, Type type, PointerFlowEdge.Kind kind) {
        if (pointerFlowGraph.addEdge(from, to, type, kind)) {
            if (!from.getPointsToSet().isEmpty()) {
                workList.addPointerEntry(to, from.getPointsToSet());
            }
        }
    }

    /**
     * Adds an edge "from -> to" to the PFG.
     */
    private void addPFGEdge(Pointer from, Pointer to, PointerFlowEdge.Kind kind) {
        addPFGEdge(from, to, null, kind);
    }

    /**
     * Processes new reachable context-sensitive method.
     */
    private void processNewCSMethod(CSMethod csMethod) {
        if (callGraph.addNewMethod(csMethod)) {
            processNewMethod(csMethod.getMethod());
            StatementProcessor processor = new StatementProcessor(csMethod);
            csMethod.getMethod()
                    .getStatements()
                    .forEach(s -> s.accept(processor));
        }
    }

    /**
     * Processes instance stores when points-to set of the base variable changes.
     *
     * @param baseVar the base variable
     * @param pts     set of new discovered objects pointed by the variable.
     */
    private void processInstanceStore(CSVariable baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Variable var = baseVar.getVariable();
        for (InstanceStore store : var.getInstanceStores()) {
            CSVariable from = dataManager.getCSVariable(context, store.getFrom());
            for (CSObj baseObj : pts) {
                InstanceField instField = dataManager.getInstanceField(
                        baseObj, store.getField());
                addPFGEdge(from, instField, PointerFlowEdge.Kind.INSTANCE_STORE);
            }
        }
    }

    /**
     * Processes instance loads when points-to set of the base variable changes.
     *
     * @param baseVar the base variable
     * @param pts     set of new discovered objects pointed by the variable.
     */
    private void processInstanceLoad(CSVariable baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Variable var = baseVar.getVariable();
        for (InstanceLoad load : var.getInstanceLoads()) {
            CSVariable to = dataManager.getCSVariable(context, load.getTo());
            for (CSObj baseObj : pts) {
                InstanceField instField = dataManager.getInstanceField(
                        baseObj, load.getField());
                addPFGEdge(instField, to, PointerFlowEdge.Kind.INSTANCE_LOAD);
            }
        }
    }

    /**
     * Processes array stores when points-to set of the array variable changes.
     *
     * @param arrayVar the array variable
     * @param pts      set of new discovered arrays pointed by the variable.
     */
    private void processArrayStore(CSVariable arrayVar, PointsToSet pts) {
        Context context = arrayVar.getContext();
        Variable var = arrayVar.getVariable();
        for (ArrayStore store : var.getArrayStores()) {
            CSVariable from = dataManager.getCSVariable(context, store.getFrom());
            for (CSObj array : pts) {
                ArrayIndex arrayIndex = dataManager.getArrayIndex(array);
                addPFGEdge(from, arrayIndex, PointerFlowEdge.Kind.ARRAY_STORE);
            }
        }
    }

    /**
     * Processes array loads when points-to set of the array variable changes.
     *
     * @param arrayVar the array variable
     * @param pts      set of new discovered arrays pointed by the variable.
     */
    private void processArrayLoad(CSVariable arrayVar, PointsToSet pts) {
        Context context = arrayVar.getContext();
        Variable var = arrayVar.getVariable();
        for (ArrayLoad load : var.getArrayLoads()) {
            CSVariable to = dataManager.getCSVariable(context, load.getTo());
            for (CSObj array : pts) {
                ArrayIndex arrayIndex = dataManager.getArrayIndex(array);
                addPFGEdge(arrayIndex, to, PointerFlowEdge.Kind.ARRAY_LOAD);
            }
        }
    }

    /**
     * Processes instance calls when points-to set of the receiver variable changes.
     *
     * @param recv the receiver variable
     * @param pts  set of new discovered objects pointed by the variable.
     */
    private void processCall(CSVariable recv, PointsToSet pts) {
        Context context = recv.getContext();
        Variable var = recv.getVariable();
        for (Call call : var.getCalls()) {
            CallSite callSite = call.getCallSite();
            for (CSObj recvObj : pts) {
                // resolve callee
                Method callee = resolveCallee(recvObj.getObject(), callSite);
                // select context
                CSCallSite csCallSite = dataManager.getCSCallSite(context, callSite);
                Context calleeContext = contextSelector.selectContext(
                        csCallSite, recvObj, callee);
                // build call edge
                CallKind callKind = getCallKind(callSite);
                CSMethod csCallee = dataManager.getCSMethod(calleeContext, callee);
                workList.addCallEdge(new Edge<>(callKind, csCallSite, csCallee));
                // pass receiver object to *this* variable
                CSVariable thisVar = dataManager.getCSVariable(
                        calleeContext, callee.getThis());
                workList.addPointerEntry(thisVar,
                        setFactory.makePointsToSet(recvObj));
            }
        }
    }

    /**
     * Process the call edges in work list.
     */
    private void processCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (!callGraph.containsEdge(edge)) {
            callGraph.addEdge(edge);
            CSMethod csCallee = edge.getCallee();
            processNewCSMethod(csCallee);
            Context callerCtx = edge.getCallSite().getContext();
            CallSite callSite = edge.getCallSite().getCallSite();
            Context calleeCtx = csCallee.getContext();
            Method callee = csCallee.getMethod();
            // pass arguments to parameters
            List<Variable> args = callSite.getArguments();
            List<Variable> params = callee.getParameters();
            for (int i = 0; i < args.size(); ++i) {
                if (args.get(i) == null) {
                    continue; // args[i] is of primitive type, skipped
                }
                CSVariable arg = dataManager.getCSVariable(callerCtx, args.get(i));
                CSVariable param = dataManager.getCSVariable(calleeCtx, params.get(i));
                addPFGEdge(arg, param, PointerFlowEdge.Kind.PARAMETER_PASSING);
            }
            // pass results to LHS variable
            if (callSite.getCall().getLHS() != null) {
                CSVariable lhs = dataManager.getCSVariable(
                        callerCtx, callSite.getCall().getLHS());
                for (Variable ret : callee.getReturnVariables()) {
                    CSVariable csRet = dataManager.getCSVariable(calleeCtx, ret);
                    addPFGEdge(csRet, lhs, PointerFlowEdge.Kind.RETURN);
                }
            }
        }
    }

    /**
     * Processes new reachable methods.
     */
    private void processNewMethod(Method method) {
        if (reachableMethods.add(method)) {
            method.getStatements()
                    .forEach(s -> s.accept(classInitializer));
        }
    }

    /**
     * Resolves callee by given receiver object and call site.
     */
    private Method resolveCallee(Obj recvObj, CallSite callSite) {
        Type type = recvObj.getType();
        if (callSite.isInterface() || callSite.isVirtual()) {
            return programManager.resolveInterfaceOrVirtualCall(
                    type, callSite.getMethod());
        } else if (callSite.isSpecial()) {
            return programManager.resolveSpecialCall(
                    callSite, callSite.getContainerMethod());
        } else {
            throw new AnalysisException("Unknown CallSite: " + callSite);
        }
    }

    private CallKind getCallKind(CallSite callSite) {
        if (callSite.isInterface()) {
            return CallKind.INTERFACE;
        } else if (callSite.isVirtual()) {
            return CallKind.VIRTUAL;
        } else if (callSite.isSpecial()) {
            return CallKind.SPECIAL;
        } else if (callSite.isStatic()) {
            return CallKind.STATIC;
        } else {
            throw new AnalysisException("Unknown call site: " + callSite);
        }
    }

    /**
     * Triggers the analysis of class initializers.
     * Well, the description of "when initialization occurs" of
     * JLS (14e, 12.4.1) and JVM Spec. (14e, 5.5) looks not
     * very consistent.
     * TODO: handles class initialization triggered by reflection,
     *  MethodHandle, and superinterfaces (that declare default methods).
     */
    private class ClassInitializer implements StatementVisitor {

        /**
         * Set of classes that have been initialized.
         */
        private Set<Type> initializedClasses = new HashSet<>();

        /**
         * Analyzes the initializer of given class.
         */
        private void initializeClass(Type cls) {
            // initialize super class
            if (cls.getSuperClass() != null) {
                initializeClass(cls.getSuperClass());
            }
            // TODO: initialize the superinterfaces which
            //  declare default methods
            Method clinit = cls.getClassInitializer();
            if (clinit != null && !initializedClasses.contains(cls)) {
                // processNewCSMethod() may trigger initialization of more
                // classes. So cls must be added before processNewCSMethod(),
                // otherwise, infinite recursion may occur.
                initializedClasses.add(cls);
                CSMethod csMethod = dataManager.getCSMethod(
                        contextSelector.getDefaultContext(), clinit);
                processNewCSMethod(csMethod);
                callGraph.addNewMethod(csMethod);
            }
        }

        @Override
        public void visit(Allocation alloc) {
            Obj obj = alloc.getObject();
            if (obj.getKind() == Obj.Kind.CLASS) {
                initializeClass((Type) obj.getAllocation());
            }
            Type type = obj.getType();
            if (type.isClassType()) {
                initializeClass(type);
            } else if (type.isArrayType()) {
                initializeClass(type.getBaseType());
            }
        }

        @Override
        public void visit(Call call) {
            CallSite callSite = call.getCallSite();
            if (callSite.isStatic()) {
                initializeClass(callSite.getMethod().getClassType());
            }
        }

        @Override
        public void visit(StaticLoad load) {
            initializeClass(load.getField().getClassType());
        }

        @Override
        public void visit(StaticStore store) {
            initializeClass(store.getField().getClassType());
        }
    }

    /**
     * Process the statements in context-sensitive new reachable methods.
     */
    private class StatementProcessor implements StatementVisitor {

        private final CSMethod csMethod;

        private final Context context;

        StatementProcessor(CSMethod csMethod) {
            this.csMethod = csMethod;
            this.context = csMethod.getContext();
        }

        @Override
        public void visit(Allocation alloc) {
            // obtain context-sensitive heap object
            Obj obj = heapModel.getObj(alloc);
            Context heapContext = contextSelector.selectHeapContext(csMethod, obj);
            CSObj csObj = dataManager.getCSObj(heapContext, obj);
            // obtain lhs variable
            CSVariable lhs = dataManager.getCSVariable(context, alloc.getVar());
            workList.addPointerEntry(lhs, setFactory.makePointsToSet(csObj));
        }

        @Override
        public void visit(Assign assign) {
            CSVariable from = dataManager.getCSVariable(context, assign.getFrom());
            CSVariable to = dataManager.getCSVariable(context, assign.getTo());
            addPFGEdge(from, to, PointerFlowEdge.Kind.LOCAL_ASSIGN);
        }

        @Override
        public void visit(AssignCast cast) {
            CSVariable from = dataManager.getCSVariable(context, cast.getFrom());
            CSVariable to = dataManager.getCSVariable(context, cast.getTo());
            addPFGEdge(from, to, cast.getType(), PointerFlowEdge.Kind.CAST);
        }

        @Override
        public void visit(Call call) {
            CallSite callSite = call.getCallSite();
            if (callSite.isStatic()) {
                Method callee = callSite.getMethod();
                CSCallSite csCallSite = dataManager.getCSCallSite(context, callSite);
                Context calleeCtx = contextSelector.selectContext(csCallSite, callee);
                CSMethod csCallee = dataManager.getCSMethod(calleeCtx, callee);
                Edge<CSCallSite, CSMethod> edge =
                        new Edge<>(CallKind.STATIC, csCallSite, csCallee);
                workList.addCallEdge(edge);
            }
        }

        @Override
        public void visit(StaticLoad load) {
            StaticField field = dataManager.getStaticField(load.getField());
            CSVariable to = dataManager.getCSVariable(context, load.getTo());
            addPFGEdge(field, to, PointerFlowEdge.Kind.STATIC_LOAD);
        }

        @Override
        public void visit(StaticStore store) {
            CSVariable from = dataManager.getCSVariable(context, store.getFrom());
            StaticField field = dataManager.getStaticField(store.getField());
            addPFGEdge(from, field, PointerFlowEdge.Kind.STATIC_STORE);
        }
    }
}
