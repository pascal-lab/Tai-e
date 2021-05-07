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

package pascal.taie.analysis.pta.core.solver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.natives.NativeModel;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;
import pascal.taie.util.AnalysisException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.language.classes.StringReps.FINALIZE;
import static pascal.taie.language.classes.StringReps.FINALIZER_REGISTER;
import static pascal.taie.util.collection.CollectionUtils.newMap;
import static pascal.taie.util.collection.CollectionUtils.newSet;

public class PointerAnalysisImpl implements PointerAnalysis {

    private static final Logger logger = LogManager.getLogger(PointerAnalysisImpl.class);

    private final ClassHierarchy hierarchy;

    private final TypeManager typeManager;

    private CSManager csManager;

    private Plugin plugin;

    private OnFlyCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private HeapModel heapModel;

    private ContextSelector contextSelector;

    private WorkList workList;

    private Set<JMethod> reachableMethods;

    private StmtProcessor stmtProcessor;

    private ClassInitializer classInitializer;

    public PointerAnalysisImpl() {
        this.typeManager = World.getTypeManager();
        this.hierarchy = World.getClassHierarchy();
    }
    
    @Override
    public ClassHierarchy getHierarchy() {
        return hierarchy;
    }

    @Override
    public HeapModel getHeapModel() {
        return heapModel;
    }

    public void setHeapModel(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    @Override
    public CSManager getCSManager() {
        return csManager;
    }

    public void setCSManager(CSManager csManager) {
        this.csManager = csManager;
    }

    @Override
    public ContextSelector getContextSelector() {
        return contextSelector;
    }

    public void setContextSelector(ContextSelector contextSelector) {
        this.contextSelector = contextSelector;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CallGraph<CSCallSite, CSMethod> getCallGraph() {
        return callGraph;
    }

    @Override
    public Stream<CSVar> vars() {
        return csManager.csVars();
    }

    @Override
    public Stream<InstanceField> instanceFields() {
        return csManager.instanceFields();
    }

    @Override
    public Stream<ArrayIndex> arrayIndexes() {
        return csManager.arrayIndexes();
    }

    @Override
    public Stream<StaticField> staticFields() {
        return csManager.staticFields();
    }

    /**
     * Run pointer analysis algorithm.
     */
    @Override
    public void analyze() {
        plugin.preprocess();
        initialize();
        solve();
        plugin.postprocess();
    }

     /**
     * Initializes pointer analysis.
     */
    private void initialize() {
        callGraph = new OnFlyCallGraph(csManager);
        pointerFlowGraph = new PointerFlowGraph();
        workList = new WorkList();
        reachableMethods = newSet();
        stmtProcessor = new StmtProcessor();
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
        NativeModel nativeModel = World.getNativeModel();
        Obj args = nativeModel.getMainArgs();
        Obj argsElem = nativeModel.getMainArgsElem();
        addArrayPointsTo(defContext, args, defContext, argsElem);
        JMethod main = World.getMainMethod();
        addVarPointsTo(defContext, main.getIR().getParam(0), defContext, args);
        plugin.initialize();
    }

    private Collection<JMethod> computeEntries() {
        List<JMethod> entries = new ArrayList<>();
        entries.add(World.getMainMethod());
        if (World.getOptions().analyzeImplicitEntries()) {
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
                if (p instanceof CSVar) {
                    CSVar v = (CSVar) p;
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
            pointerFlowGraph.outEdgesOf(pointer).forEach(edge -> {
                Pointer to = edge.getTo();
                edge.getType().ifPresentOrElse(
                        type -> addPointerEntry(to, getAssignablePointsToSet(diff, type)),
                        () -> addPointerEntry(to, diff));
            });
        }
        return diff;
    }

    @Override
    public void addVarPointsTo(Context context, Var var, Context heapContext, Obj obj) {
        CSObj csObj = csManager.getCSObj(heapContext, obj);
        addVarPointsTo(context, var, PointsToSetFactory.make(csObj));
    }

    @Override
    public void addVarPointsTo(Context context, Var var, PointsToSet pts) {
        CSVar csVar = csManager.getCSVar(context, var);
        addPointerEntry(csVar, pts);
    }

    @Override
    public void addArrayPointsTo(Context arrayContext, Obj array, Context heapContext, Obj obj) {
        CSObj csArray = csManager.getCSObj(arrayContext, array);
        ArrayIndex arrayIndex = csManager.getArrayIndex(csArray);
        CSObj elem = csManager.getCSObj(heapContext, obj);
        addPointerEntry(arrayIndex, PointsToSetFactory.make(elem));
    }

    @Override
    public void addStaticFieldPointsTo(JField field, PointsToSet pts) {
        assert field.isStatic();
        StaticField sfield = csManager.getStaticField(field);
        addPointerEntry(sfield, pts);
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
        pts.objects()
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
     * Processes instance stores when points-to set of the base variable changes.
     *
     * @param baseVar the base variable
     * @param pts     set of new discovered objects pointed by the variable.
     */
    private void processInstanceStore(CSVar baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Var var = baseVar.getVar();
        for (StoreField store : var.getStoreFields()) {
            Var fromVar = store.getRValue();
            if (isConcerned(fromVar)) {
                CSVar from = csManager.getCSVar(context, fromVar);
                for (CSObj baseObj : pts) {
                    InstanceField instField = csManager.getInstanceField(
                            baseObj, store.getFieldRef().resolve());
                    addPFGEdge(from, instField, PointerFlowEdge.Kind.INSTANCE_STORE);
                }
            }
        }
    }

    /**
     * Processes instance loads when points-to set of the base variable changes.
     *
     * @param baseVar the base variable
     * @param pts     set of new discovered objects pointed by the variable.
     */
    private void processInstanceLoad(CSVar baseVar, PointsToSet pts) {
        Context context = baseVar.getContext();
        Var var = baseVar.getVar();
        for (LoadField load : var.getLoadFields()) {
            Var toVar = load.getLValue();
            if (isConcerned(toVar)) {
                CSVar to = csManager.getCSVar(context, toVar);
                for (CSObj baseObj : pts) {
                    InstanceField instField = csManager.getInstanceField(
                            baseObj, load.getFieldRef().resolve());
                    addPFGEdge(instField, to, PointerFlowEdge.Kind.INSTANCE_LOAD);
                }
            }
        }
    }
    
        /**
     * Processes array stores when points-to set of the array variable changes.
     *
     * @param arrayVar the array variable
     * @param pts      set of new discovered arrays pointed by the variable.
     */
    private void processArrayStore(CSVar arrayVar, PointsToSet pts) {
        Context context = arrayVar.getContext();
        Var var = arrayVar.getVar();
        for (StoreArray store : var.getStoreArrays()) {
            Var rvalue = store.getRValue();
            if (isConcerned(rvalue)) {
                CSVar from = csManager.getCSVar(context, rvalue);
                for (CSObj array : pts) {
                    ArrayIndex arrayIndex = csManager.getArrayIndex(array);
                    // we need type guard for array stores as Java arrays
                    // are covariant
                    addPFGEdge(from, arrayIndex, arrayIndex.getType(),
                            PointerFlowEdge.Kind.ARRAY_STORE);
                }
            }
        }
    }

    /**
     * Processes array loads when points-to set of the array variable changes.
     *
     * @param arrayVar the array variable
     * @param pts      set of new discovered arrays pointed by the variable.
     */
    private void processArrayLoad(CSVar arrayVar, PointsToSet pts) {
        Context context = arrayVar.getContext();
        Var var = arrayVar.getVar();
        for (LoadArray load : var.getLoadArrays()) {
            Var lvalue = load.getLValue();
            if (isConcerned(lvalue)) {
                CSVar to = csManager.getCSVar(context, lvalue);
                for (CSObj array : pts) {
                    ArrayIndex arrayIndex = csManager.getArrayIndex(array);
                    addPFGEdge(arrayIndex, to, PointerFlowEdge.Kind.ARRAY_LOAD);
                }
            }
        }
    }
    
    /**
     * Processes instance calls when points-to set of the receiver variable changes.
     *
     * @param recv the receiver variable
     * @param pts  set of new discovered objects pointed by the variable.
     */
    private void processCall(CSVar recv, PointsToSet pts) {
        Context context = recv.getContext();
        Var var = recv.getVar();
        for (Invoke invoke : var.getInvokes()) {
            InvokeExp callSite = invoke.getInvokeExp();
            for (CSObj recvObj : pts) {
                // resolve callee
                JMethod callee = resolveCallee(
                        recvObj.getObject().getType(), callSite);
                if (callee != null) {
                    // select context
                    CSCallSite csCallSite = csManager.getCSCallSite(context, callSite);
                    Context calleeContext = contextSelector.selectContext(
                            csCallSite, recvObj, callee);
                    // build call edge
                    CSMethod csCallee = csManager.getCSMethod(calleeContext, callee);
                    workList.addCallEdge(new Edge<>(
                            getCallKind(callSite), csCallSite, csCallee));
                    // pass receiver object to *this* variable
                    CSVar thisVar = csManager.getCSVar(
                            calleeContext, callee.getIR().getThis());
                    addPointerEntry(thisVar, PointsToSetFactory.make(recvObj));
                }
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
            InvokeExp callSite = edge.getCallSite().getCallSite();
            Context calleeCtx = csCallee.getContext();
            JMethod callee = csCallee.getMethod();
            // pass arguments to parameters
            for (int i = 0; i < callSite.getArgCount(); ++i) {
                Var arg = callSite.getArg(i);
                if (isConcerned(arg)) {
                    Var param = callee.getIR().getParam(i);
                    CSVar argVar = csManager.getCSVar(callerCtx, arg);
                    CSVar paramVar = csManager.getCSVar(calleeCtx, param);
                    addPFGEdge(argVar, paramVar, PointerFlowEdge.Kind.PARAMETER_PASSING);
                }
            }
            // pass results to LHS variable
            Invoke invoke = (Invoke) callSite.getCallSite().getStmt();
            Var lhs = invoke.getResult();
            if (lhs != null && isConcerned(lhs)) {
                CSVar csLHS = csManager.getCSVar(callerCtx, lhs);
                for (Var ret : callee.getIR().getReturnVars()) {
                    if (isConcerned(ret)) {
                        CSVar csRet = csManager.getCSVar(calleeCtx, ret);
                        addPFGEdge(csRet, csLHS, PointerFlowEdge.Kind.RETURN);
                    }
                }
            }
            plugin.handleNewCallEdge(edge);
        }
    }

    /**
     * Processes new reachable context-sensitive method.
     */
    private void processNewCSMethod(CSMethod csMethod) {
        if (callGraph.addNewMethod(csMethod)) {
            processNewMethod(csMethod.getMethod());
            stmtProcessor.setCSMethod(csMethod);
            csMethod.getMethod()
                    .getIR()
                    .getStmts()
                    .forEach(s -> s.accept(stmtProcessor));
            plugin.handleNewCSMethod(csMethod);
        }
    }

    /**
     * Processes new reachable methods.
     */
    private void processNewMethod(JMethod method) {
        if (reachableMethods.add(method)) {
            plugin.handleNewMethod(method);
            method.getIR().getStmts()
                    .forEach(s -> s.accept(classInitializer));
        }
    }

    private JMethod resolveCallee(Type type, InvokeExp callSite) {
        MethodRef methodRef = callSite.getMethodRef();
        if (callSite instanceof InvokeVirtual ||
                callSite instanceof InvokeInterface) {
            return hierarchy.dispatch(type, methodRef);
        } else if (callSite instanceof InvokeSpecial ||
                callSite instanceof InvokeStatic) {
            return methodRef.resolve();
        } else {
            throw new AnalysisException("Cannot resolve InvokeExp: " + callSite);
        }
    }

    private static CallKind getCallKind(InvokeExp invokeExp) {
        if (invokeExp instanceof InvokeVirtual) {
            return CallKind.VIRTUAL;
        } else if (invokeExp instanceof InvokeInterface) {
            return CallKind.INTERFACE;
        } else if (invokeExp instanceof InvokeSpecial) {
            return CallKind.SPECIAL;
        } else if (invokeExp instanceof InvokeStatic) {
            return CallKind.STATIC;
        } else {
            throw new AnalysisException("Cannot handle InvokeExp: " + invokeExp);
        }
    }

    /**
     * @return if the type of given expression is concerned in pointer analysis.
     */
    private static boolean isConcerned(Exp exp) {
        Type type = exp.getType();
        return type instanceof ReferenceType && !(type instanceof NullType);
    }

    /**
     * Process the statements in context-sensitive new reachable methods.
     */
    private class StmtProcessor implements StmtVisitor {

        private CSMethod csMethod;

        private Context context;

        private final Map<NewMultiArray, NewArray[]> newArrays = newMap();

        private final JMethod finalize = hierarchy.getJREMethod(FINALIZE);

        private final MethodRef finalizeRef = finalize.getRef();

        private final MethodRef registerRef = hierarchy
                .getJREMethod(FINALIZER_REGISTER).getRef();

        private final Map<New, InvokeStatic> registerInvokes = newMap();

        private void setCSMethod(CSMethod csMethod) {
            this.csMethod = csMethod;
            this.context = csMethod.getContext();
        }

        @Override
        public void visit(New stmt) {
            // obtain context-sensitive heap object
            NewExp rvalue = stmt.getRValue();
            Obj obj = heapModel.getObj(rvalue);
            Context heapContext = contextSelector.selectHeapContext(csMethod, obj);
            addVarPointsTo(context, stmt.getLValue(), heapContext, obj);
            if (rvalue instanceof NewMultiArray) {
                processNewMultiArray((NewMultiArray) rvalue, heapContext, obj);
            }
            if (hasOverriddenFinalize(rvalue)) {
                processFinalizer(stmt);
            }
        }

        private void processNewMultiArray(NewMultiArray newMultiArray,
                                          Context arrayContext, Obj array) {
            NewArray[] arrays = newArrays.computeIfAbsent(newMultiArray, nma -> {
                ArrayType type = nma.getType();
                NewArray[] newArrays = new NewArray[nma.getLengthCount() - 1];
                for (int i = 1; i < nma.getLengthCount(); ++i) {
                    type = (ArrayType) type.getElementType();
                    NewArray newArray = new NewArray(type, nma.getLength(i));
                    newArray.setAllocationSite(nma.getAllocationSite());
                    newArrays[i - 1] = newArray;
                }
                return newArrays;
            });
            for (NewArray newArray : arrays) {
                Obj elem = heapModel.getObj(newArray);
                Context elemContext = contextSelector
                        .selectHeapContext(csMethod, elem);
                addArrayPointsTo(arrayContext, array, elemContext, elem);
                array = elem;
                arrayContext = elemContext;
            }
        }

        private boolean hasOverriddenFinalize(NewExp newExp) {
            return !hierarchy.dispatch(newExp.getType(), finalizeRef)
                    .equals(finalize);
        }

        /**
         * Call Finalizer.register() at allocation sites of objects which override
         * Object.finalize() method.
         * NOTE: finalize() has been deprecated starting with Java 9, and will
         * eventually be removed.
         */
        private void processFinalizer(New stmt) {
            InvokeStatic registerInvoke = registerInvokes.computeIfAbsent(stmt, s -> {
                InvokeStatic callSite = new InvokeStatic(registerRef,
                        Collections.singletonList(s.getLValue()));
                Invoke invoke = new Invoke(csMethod.getMethod(), callSite);
                invoke.setLineNumber(stmt.getLineNumber());
                return callSite;
            });
            processInvokeStatic(registerInvoke);
        }

        private void processInvokeStatic(InvokeStatic callSite) {
            JMethod callee = resolveCallee(null, callSite);
            CSCallSite csCallSite = csManager.getCSCallSite(context, callSite);
            Context calleeCtx = contextSelector.selectContext(csCallSite, callee);
            CSMethod csCallee = csManager.getCSMethod(calleeCtx, callee);
            Edge<CSCallSite, CSMethod> edge =
                    new Edge<>(CallKind.STATIC, csCallSite, csCallee);
            workList.addCallEdge(edge);
        }

        @Override
        public void visit(AssignLiteral stmt) {
            Literal literal = stmt.getRValue();
            if (isConcerned(literal)) {
                Obj obj = heapModel.getConstantObj((ReferenceLiteral) literal);
                Context heapContext = contextSelector
                        .selectHeapContext(csMethod, obj);
                addVarPointsTo(context, stmt.getLValue(), heapContext, obj);
            }
        }

        @Override
        public void visit(Copy stmt) {
            Var rvalue = stmt.getRValue();
            if (isConcerned(rvalue)) {
                CSVar from = csManager.getCSVar(context, rvalue);
                CSVar to = csManager.getCSVar(context, stmt.getLValue());
                addPFGEdge(from, to, PointerFlowEdge.Kind.LOCAL_ASSIGN);
            }
        }

        @Override
        public void visit(Cast stmt) {
            CastExp cast = stmt.getRValue();
            if (isConcerned(cast.getValue())) {
                CSVar from = csManager.getCSVar(context, cast.getValue());
                CSVar to = csManager.getCSVar(context, stmt.getLValue());
                addPFGEdge(from, to, cast.getType(), PointerFlowEdge.Kind.CAST);
            }
        }

        /**
         * Process static load.
         */
        @Override
        public void visit(LoadField stmt) {
            if (stmt.isStatic() && isConcerned(stmt.getRValue())) {
                JField field = stmt.getFieldRef().resolve();
                StaticField sfield = csManager.getStaticField(field);
                CSVar to = csManager.getCSVar(context, stmt.getLValue());
                addPFGEdge(sfield, to, PointerFlowEdge.Kind.STATIC_LOAD);
            }
        }

        /**
         * Process static store.
         */
        @Override
        public void visit(StoreField stmt) {
            if (stmt.isStatic() && isConcerned(stmt.getRValue())) {
                JField field = stmt.getFieldRef().resolve();
                StaticField sfield = csManager.getStaticField(field);
                CSVar from = csManager.getCSVar(context, stmt.getRValue());
                addPFGEdge(from, sfield, PointerFlowEdge.Kind.STATIC_STORE);
            }
        }

        /**
         * Process static invocation.
         */
        @Override
        public void visit(Invoke stmt) {
            if (stmt.isStatic()) {
                processInvokeStatic((InvokeStatic) stmt.getInvokeExp());
            }
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
    private class ClassInitializer implements StmtVisitor {

        /**
         * Set of classes that have been initialized.
         */
        private final Set<JClass> initializedClasses = newSet();

        @Override
        public void visit(New stmt) {
            initializeClass(extractClass(stmt.getRValue().getType()));
        }

        @Override
        public void visit(AssignLiteral stmt) {
            Literal rvalue = stmt.getRValue();
            if (isConcerned(rvalue)) {
                initializeClass(extractClass(rvalue.getType()));
                if (rvalue instanceof ClassLiteral) {
                    initializeClass(extractClass(
                            ((ClassLiteral) rvalue).getTypeValue()));
                }
            }
        }

        /**
         * Analyzes the initializer of given class.
         */
        private void initializeClass(JClass cls) {
            if (cls == null || initializedClasses.contains(cls)) {
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
        public void visit(Invoke stmt) {
            if (!(stmt.getInvokeExp() instanceof InvokeDynamic)) {
                processMemberRef(stmt.getMethodRef());
            }
            // TODO: check if the declaring class of bootstrap method
            //  of invokedynamic instruction needs to be initialized
        }

        @Override
        public void visit(LoadField stmt) {
            processMemberRef(stmt.getFieldRef());
        }

        @Override
        public void visit(StoreField stmt) {
            processMemberRef(stmt.getFieldRef());
        }

        private void processMemberRef(MemberRef memberRef) {
            if (memberRef.isStatic()) {
                initializeClass(memberRef.resolve().getDeclaringClass());
            }
        }
    }
}
