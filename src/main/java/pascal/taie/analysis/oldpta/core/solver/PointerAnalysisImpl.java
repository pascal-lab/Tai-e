/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.oldpta.core.solver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.oldpta.core.context.Context;
import pascal.taie.analysis.oldpta.core.context.ContextSelector;
import pascal.taie.analysis.oldpta.plugin.Plugin;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.ArrayType;
import pascal.taie.language.types.ClassType;
import pascal.taie.language.types.ReferenceType;
import pascal.taie.language.types.Type;
import pascal.taie.language.types.TypeManager;
import pascal.taie.analysis.oldpta.core.cs.ArrayIndex;
import pascal.taie.analysis.oldpta.core.cs.CSCallSite;
import pascal.taie.analysis.oldpta.core.cs.CSManager;
import pascal.taie.analysis.oldpta.core.cs.CSMethod;
import pascal.taie.analysis.oldpta.core.cs.CSObj;
import pascal.taie.analysis.oldpta.core.cs.CSVariable;
import pascal.taie.analysis.oldpta.core.cs.InstanceField;
import pascal.taie.analysis.oldpta.core.cs.Pointer;
import pascal.taie.analysis.oldpta.core.cs.StaticField;
import pascal.taie.analysis.oldpta.core.heap.HeapModel;
import pascal.taie.analysis.oldpta.env.Environment;
import pascal.taie.analysis.oldpta.ir.Allocation;
import pascal.taie.analysis.oldpta.ir.ArrayLoad;
import pascal.taie.analysis.oldpta.ir.ArrayStore;
import pascal.taie.analysis.oldpta.ir.Assign;
import pascal.taie.analysis.oldpta.ir.AssignCast;
import pascal.taie.analysis.oldpta.ir.Call;
import pascal.taie.analysis.oldpta.ir.CallSite;
import pascal.taie.analysis.oldpta.ir.InstanceLoad;
import pascal.taie.analysis.oldpta.ir.InstanceStore;
import pascal.taie.analysis.oldpta.ir.Obj;
import pascal.taie.analysis.oldpta.ir.StatementVisitor;
import pascal.taie.analysis.oldpta.ir.StaticLoad;
import pascal.taie.analysis.oldpta.ir.StaticStore;
import pascal.taie.analysis.oldpta.ir.Variable;
import pascal.taie.analysis.oldpta.set.PointsToSet;
import pascal.taie.analysis.oldpta.set.PointsToSetFactory;
import pascal.taie.analysis.pta.PTAOptions;
import pascal.taie.util.AnalysisException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.newSet;

public class PointerAnalysisImpl implements PointerAnalysis {

    private static final Logger logger = LogManager.getLogger(PointerAnalysisImpl.class);

    private final ClassHierarchy hierarchy;

    private final TypeManager typeManager;

    private final Environment environment;

    private CSManager csManager;

    private Plugin plugin;

    private OnFlyCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private HeapModel heapModel;

    private ContextSelector contextSelector;

    private WorkList workList;

    private Set<JMethod> reachableMethods;

    private ClassInitializer classInitializer;

    public PointerAnalysisImpl() {
        this.typeManager = World.getTypeManager();
        this.hierarchy = World.getClassHierarchy();
        this.environment = World.getEnvironment();
    }

    @Override
    public ClassHierarchy getHierarchy() {
        return hierarchy;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public CSManager getCSManager() {
        return csManager;
    }

    void setCSManager(CSManager csManager) {
        this.csManager = csManager;
    }

    void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ContextSelector getContextSelector() {
        return contextSelector;
    }

    void setContextSelector(ContextSelector contextSelector) {
        this.contextSelector = contextSelector;
    }

    void setHeapModel(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    @Override
    public CallGraph<CSCallSite, CSMethod> getCallGraph() {
        return callGraph;
    }

    @Override
    public Stream<CSVariable> getVariables() {
        return csManager.getCSVariables();
    }

    @Override
    public Stream<InstanceField> getInstanceFields() {
        return csManager.getInstanceFields();
    }

    @Override
    public Stream<ArrayIndex> getArrayIndexes() {
        return csManager.getArrayIndexes();
    }

    @Override
    public Stream<StaticField> getStaticFields() {
        return csManager.getStaticFields();
    }

    /**
     * Runs pointer analysis algorithm.
     */
    @Override
    public void analyze() {
        plugin.preprocess();
        initialize();
        solve();
        plugin.postprocess();
    }

    @Override
    public void addPointsTo(Context context, Variable var,
                            Context heapContext, Obj obj) {
        // TODO: use heapModel to process obj?
        CSObj csObj = csManager.getCSObj(heapContext, obj);
        addPointsTo(context, var, PointsToSetFactory.make(csObj));
    }

    @Override
    public void addPointsTo(Context context, Variable var, PointsToSet pts) {
        CSVariable csVar = csManager.getCSVariable(context, var);
        addPointerEntry(csVar, pts);
    }

    @Override
    public void addPointsTo(Context arrayContext, Obj array,
                            Context heapContext, Obj obj) {
        CSObj csArray = csManager.getCSObj(arrayContext, array);
        ArrayIndex arrayIndex = csManager.getArrayIndex(csArray);
        CSObj elem = csManager.getCSObj(heapContext, obj);
        addPointerEntry(arrayIndex, PointsToSetFactory.make(elem));
    }

    /**
     * Initializes pointer analysis.
     */
    private void initialize() {
        callGraph = new OnFlyCallGraph(csManager);
        pointerFlowGraph = new PointerFlowGraph();
        workList = new WorkList();
        reachableMethods = newSet();
        classInitializer = new ClassInitializer();

        // process program entries (including implicit entries)
        Context defContext = contextSelector.getDefaultContext();
        for (JMethod entry : computeEntries()) {
            // initialize class type of entry methods
            classInitializer.initializeClass(entry.getDeclaringClass());
            CSMethod csMethod = csManager.getCSMethod(defContext, entry);
            callGraph.addEntryMethod(csMethod);
            processNewCSMethod(csMethod);
        }
        // setup main arguments
        Obj args = environment.getMainArgs();
        Obj argsElem = environment.getMainArgsElem();
        addPointsTo(defContext, args, defContext, argsElem);
        JMethod main = World.getMainMethod();
        addPointsTo(defContext, main.getPTAIR().getParam(0), defContext, args);
        plugin.initialize();
    }

    private Collection<JMethod> computeEntries() {
        List<JMethod> entries = new ArrayList<>();
        entries.add(World.getMainMethod());
        if (PTAOptions.get().analyzeImplicitEntries()) {
            entries.addAll(World.getImplicitEntries());
        }
        return entries;
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
                    plugin.handleNewPointsToSet(v, diff);
                }
            }
            while (workList.hasCallEdges()) {
                processCallEdge(workList.pollCallEdge());
            }
        }
        plugin.finish();
    }

    /**
     * Propagates pointsToSet to pt(pointer) and its PFG successors,
     * returns the difference set of pointsToSet and pt(pointer).
     */
    private PointsToSet propagate(Pointer pointer, PointsToSet pointsToSet) {
        logger.trace("Propagate {} to {}", pointsToSet, pointer);
        final PointsToSet diff = PointsToSetFactory.make();
        for (CSObj obj : pointsToSet) {
            if (pointer.getPointsToSet().addObject(obj)) {
                diff.addObject(obj);
            }
        }
        if (!diff.isEmpty()) {
            for (PointerFlowEdge edge : pointerFlowGraph.getOutEdgesOf(pointer)) {
                Pointer to = edge.getTo();
                // TODO: use Optional.ifPresentOrElse() after upgrade to Java 9+
                if (edge.getType().isPresent()) {
                    // Checks assignable objects
                    addPointerEntry(to,
                            getAssignablePointsToSet(diff, edge.getType().get()));
                } else {
                    addPointerEntry(to, diff);
                }
            }
        }
        return diff;
    }

    /**
     * Add a <pointer, pointsToSet> entry to work-list.
     */
    private void addPointerEntry(Pointer pointer, PointsToSet pointsToSet) {
        workList.addPointerEntry(pointer, pointsToSet);
    }

    /**
     * Given a points-to set pts and a type t, returns the objects of pts
     * which can be assigned to t.
     */
    private PointsToSet getAssignablePointsToSet(PointsToSet pts, Type type) {
        PointsToSet result = PointsToSetFactory.make();
        pts.stream()
                .filter(o -> typeManager.isSubtype(type, o.getObject().getType()))
                .forEach(result::addObject);
        return result;
    }

    /**
     * Adds an edge "from -> to" to the PFG.
     * If type is not null, then we need to filter out assignable objects
     * in from points-to set.
     */
    private void addPFGEdge(Pointer from, Pointer to, Type type, PointerFlowEdge.Kind kind) {
        if (pointerFlowGraph.addEdge(from, to, type, kind)) {
            PointsToSet fromSet = type == null ?
                    from.getPointsToSet() :
                    getAssignablePointsToSet(from.getPointsToSet(), type);
            if (!fromSet.isEmpty()) {
                addPointerEntry(to, fromSet);
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
                    .getPTAIR()
                    .getStatements()
                    .forEach(s -> s.accept(processor));
            plugin.handleNewCSMethod(csMethod);
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
            CSVariable from = csManager.getCSVariable(context, store.getFrom());
            for (CSObj baseObj : pts) {
                InstanceField instField = csManager.getInstanceField(
                        baseObj, store.getFieldRef().resolve());
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
            CSVariable to = csManager.getCSVariable(context, load.getTo());
            for (CSObj baseObj : pts) {
                InstanceField instField = csManager.getInstanceField(
                        baseObj, load.getFieldRef().resolve());
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
            CSVariable from = csManager.getCSVariable(context, store.getFrom());
            for (CSObj array : pts) {
                ArrayIndex arrayIndex = csManager.getArrayIndex(array);
                // we need type guard for array stores as Java arrays
                // are covariant
                addPFGEdge(from, arrayIndex, arrayIndex.getType(),
                        PointerFlowEdge.Kind.ARRAY_STORE);
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
            CSVariable to = csManager.getCSVariable(context, load.getTo());
            for (CSObj array : pts) {
                ArrayIndex arrayIndex = csManager.getArrayIndex(array);
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
                JMethod callee = resolveCallee(
                        recvObj.getObject().getType(), callSite);
                // select context
                CSCallSite csCallSite = csManager.getCSCallSite(context, callSite);
                Context calleeContext = contextSelector.selectContext(
                        csCallSite, recvObj, callee);
                // build call edge
                CSMethod csCallee = csManager.getCSMethod(calleeContext, callee);
                workList.addCallEdge(new Edge<>(
                        callSite.getKind(), csCallSite, csCallee));
                // pass receiver object to *this* variable
                CSVariable thisVar = csManager.getCSVariable(
                        calleeContext, callee.getPTAIR().getThis());
                addPointerEntry(thisVar, PointsToSetFactory.make(recvObj));
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
            JMethod callee = csCallee.getMethod();
            // pass arguments to parameters
            for (int i = 0; i < callSite.getArgCount(); ++i) {
                Variable arg = callSite.getArg(i);
                if (arg != null) {
                    assert arg.getType() instanceof ReferenceType;
                    Variable param = callee.getPTAIR().getParam(i);
                    CSVariable argVar = csManager.getCSVariable(callerCtx, arg);
                    CSVariable paramVar = csManager.getCSVariable(calleeCtx, param);
                    addPFGEdge(argVar, paramVar, PointerFlowEdge.Kind.PARAMETER_PASSING);
                }
            }
            // pass results to LHS variable
            callSite.getCall().getLHS().ifPresent(lhs -> {
                CSVariable csLHS = csManager.getCSVariable(callerCtx, lhs);
                for (Variable ret : callee.getPTAIR().getReturnVariables()) {
                    CSVariable csRet = csManager.getCSVariable(calleeCtx, ret);
                    addPFGEdge(csRet, csLHS, PointerFlowEdge.Kind.RETURN);
                }
            });
        }
    }

    /**
     * Processes new reachable methods.
     */
    private void processNewMethod(JMethod method) {
        if (reachableMethods.add(method)) {
            plugin.handleNewMethod(method);
            method.getPTAIR().getStatements()
                    .forEach(s -> s.accept(classInitializer));
        }
    }

    private JMethod resolveCallee(Type type, CallSite callSite) {
        MethodRef methodRef = callSite.getMethodRef();
        switch (callSite.getKind()) {
            case VIRTUAL:
            case INTERFACE:
                return hierarchy.dispatch(type, methodRef);
            case SPECIAL:
            case STATIC:
                return methodRef.resolve();
            default:
                throw new AnalysisException("Unknown CallSite: " + callSite);
        }
    }

    /**
     * Triggers the analysis of class initializers.
     * Well, the description of "when initialization occurs" of
     * JLS (11 Ed., 12.4.1) and JVM Spec. (11 Ed., 5.5) looks not
     * very consistent.
     * TODO: handles class initialization triggered by reflection,
     *  MethodHandle, and superinterfaces (that declare default methods).
     */
    private class ClassInitializer implements StatementVisitor {

        /**
         * Set of classes that have been initialized.
         */
        private final Set<JClass> initializedClasses = newSet();

        /**
         * Analyzes the initializer of given class.
         * @param cls
         */
        private void initializeClass(JClass cls) {
            if (cls == null) {
                return;
            }

            if (initializedClasses.contains(cls)) {
                // cls has already been initialized
                return;
            }

            // initialize super class
            JClass superclass = cls.getSuperClass();
            if (superclass != null) {
                initializeClass(superclass);
            }
            // TODO: initialize the superinterfaces which
            //  declare default methods
            JMethod clinit = cls.getClinit();
            if (clinit != null) {
                // processNewCSMethod() may trigger initialization of more
                // classes. So cls must be added before processNewCSMethod(),
                // otherwise, infinite recursion may occur.
                initializedClasses.add(cls);
                CSMethod csMethod = csManager.getCSMethod(
                        contextSelector.getDefaultContext(), clinit);
                processNewCSMethod(csMethod);
            }
        }

        @Override
        public void visit(Allocation alloc) {
            Obj obj = alloc.getObject();
            if (obj.getKind() == Obj.Kind.CLASS) {
                initializeClass(extractClass((Type) obj.getAllocation()));
            }
            initializeClass(extractClass(obj.getType()));
        }

        /**
         * Extract the class to be initialized from given type.
         */
        private JClass extractClass(Type type) {
            if (type instanceof ClassType) {
                return ((ClassType) type).getJClass();
            } else if (type instanceof ArrayType) {
                return extractClass(((ArrayType) type).getBaseType());
            }
            // Some types do not contain class to be initialized,
            // e.g., int[], then return null for such cases.
            return null;
        }

        @Override
        public void visit(Call call) {
            CallSite callSite = call.getCallSite();
            if (callSite.getKind() == CallKind.STATIC) {
                JMethod method = callSite.getMethodRef().resolve();
                initializeClass(method.getDeclaringClass());
            }
        }

        @Override
        public void visit(StaticLoad load) {
            JField field = load.getFieldRef().resolve();
            initializeClass(field.getDeclaringClass());
        }

        @Override
        public void visit(StaticStore store) {
            JField field = store.getFieldRef().resolve();
            initializeClass(field.getDeclaringClass());
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
            addPointsTo(context, alloc.getVar(), heapContext, obj);
        }

        @Override
        public void visit(Assign assign) {
            CSVariable from = csManager.getCSVariable(context, assign.getFrom());
            CSVariable to = csManager.getCSVariable(context, assign.getTo());
            addPFGEdge(from, to, PointerFlowEdge.Kind.LOCAL_ASSIGN);
        }

        @Override
        public void visit(AssignCast cast) {
            CSVariable from = csManager.getCSVariable(context, cast.getFrom());
            CSVariable to = csManager.getCSVariable(context, cast.getTo());
            addPFGEdge(from, to, cast.getType(), PointerFlowEdge.Kind.CAST);
        }

        @Override
        public void visit(Call call) {
            CallSite callSite = call.getCallSite();
            if (callSite.getKind() == CallKind.STATIC) {
                JMethod callee = resolveCallee(null, callSite);
                CSCallSite csCallSite = csManager.getCSCallSite(context, callSite);
                Context calleeCtx = contextSelector.selectContext(csCallSite, callee);
                CSMethod csCallee = csManager.getCSMethod(calleeCtx, callee);
                Edge<CSCallSite, CSMethod> edge =
                        new Edge<>(CallKind.STATIC, csCallSite, csCallee);
                workList.addCallEdge(edge);
            }
        }

        @Override
        public void visit(StaticLoad load) {
            JField jfield = load.getFieldRef().resolve();
            StaticField field = csManager.getStaticField(jfield);
            CSVariable to = csManager.getCSVariable(context, load.getTo());
            addPFGEdge(field, to, PointerFlowEdge.Kind.STATIC_LOAD);
        }

        @Override
        public void visit(StaticStore store) {
            CSVariable from = csManager.getCSVariable(context, store.getFrom());
            JField jfield = store.getFieldRef().resolve();
            StaticField field = csManager.getStaticField(jfield);
            addPFGEdge(from, field, PointerFlowEdge.Kind.STATIC_STORE);
        }
    }
}
