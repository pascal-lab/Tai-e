/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.newpta.core.solver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.callgraph.CallGraph;
import pascal.taie.callgraph.CallKind;
import pascal.taie.callgraph.Edge;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MemberRef;
import pascal.taie.java.classes.MethodRef;
import pascal.taie.java.types.ArrayType;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.ReferenceType;
import pascal.taie.java.types.Type;
import pascal.taie.newpta.core.context.Context;
import pascal.taie.newpta.core.context.ContextSelector;
import pascal.taie.newpta.core.cs.ArrayIndex;
import pascal.taie.newpta.core.cs.CSCallSite;
import pascal.taie.newpta.core.cs.CSManager;
import pascal.taie.newpta.core.cs.CSMethod;
import pascal.taie.newpta.core.cs.CSObj;
import pascal.taie.newpta.core.cs.CSVar;
import pascal.taie.newpta.core.cs.InstanceField;
import pascal.taie.newpta.core.cs.Pointer;
import pascal.taie.newpta.core.cs.StaticField;
import pascal.taie.newpta.core.heap.EnvObjs;
import pascal.taie.newpta.core.heap.HeapModel;
import pascal.taie.newpta.core.heap.Obj;
import pascal.taie.newpta.plugin.Plugin;
import pascal.taie.newpta.set.PointsToSet;
import pascal.taie.newpta.set.PointsToSetFactory;
import pascal.taie.pta.PTAOptions;
import pascal.taie.util.AnalysisException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.CollectionUtils.newSet;

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
    public Stream<CSVar> getVars() {
        return csManager.getCSVars();
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
        EnvObjs envObjs = heapModel.getEnvObjs();
        Obj args = envObjs.getMainArgs();
        Obj argsElem = envObjs.getMainArgsElem();
        addPointsTo(defContext, args, defContext, argsElem);
        JMethod main = World.getMainMethod();
        addPointsTo(defContext, main.getNewIR().getParam(0), defContext, args);
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
                if (p instanceof CSVar) {
                    CSVar v = (CSVar) p;
//                    processInstanceStore(v, diff);
//                    processInstanceLoad(v, diff);
//                    processArrayStore(v, diff);
//                    processArrayLoad(v, diff);
//                    processCall(v, diff);
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

    @Override
    public void addPointsTo(Context context, Var var, Context heapContext, Obj obj) {
        CSObj csObj = csManager.getCSObj(heapContext, obj);
        addPointsTo(context, var, PointsToSetFactory.make(csObj));
    }

    @Override
    public void addPointsTo(Context context, Var var, PointsToSet pts) {
        CSVar csVar = csManager.getCSVar(context, var);
        addPointerEntry(csVar, pts);
    }

    @Override
    public void addPointsTo(Context arrayContext, Obj array, Context heapContext, Obj obj) {
        CSObj csArray = csManager.getCSObj(arrayContext, array);
        ArrayIndex arrayIndex = csManager.getArrayIndex(csArray);
        CSObj elem = csManager.getCSObj(heapContext, obj);
        addPointerEntry(arrayIndex, PointsToSetFactory.make(elem));
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
                if (arg.getType() instanceof ReferenceType) {
                    Var param = callee.getNewIR().getParam(i);
                    CSVar argVar = csManager.getCSVar(callerCtx, arg);
                    CSVar paramVar = csManager.getCSVar(calleeCtx, param);
                    addPFGEdge(argVar, paramVar, PointerFlowEdge.Kind.PARAMETER_PASSING);
                }
            }
            // pass results to LHS variable
            Invoke invoke = (Invoke) callSite.getCallSite().getStmt();
            Var lhs = invoke.getResult();
            if (lhs != null) {
                CSVar csLHS = csManager.getCSVar(callerCtx, lhs);
                for (Var ret : callee.getNewIR().getReturnVars()) {
                    CSVar csRet = csManager.getCSVar(calleeCtx, ret);
                    addPFGEdge(csRet, csLHS, PointerFlowEdge.Kind.RETURN);
                }
            }
        }
    }

    /**
     * Processes new reachable context-sensitive method.
     */
    private void processNewCSMethod(CSMethod csMethod) {
        if (csMethod.getMethod().isNative()) {
            // TODO: handle native methods
            return;
        }
        if (callGraph.addNewMethod(csMethod)) {
            processNewMethod(csMethod.getMethod());
            StmtProcessor processor = new StmtProcessor(csMethod);
            csMethod.getMethod()
                    .getNewIR()
                    .getStmts()
                    .forEach(s -> s.accept(processor));
            plugin.handleNewCSMethod(csMethod);
        }
    }

    /**
     * Processes new reachable methods.
     */
    private void processNewMethod(JMethod method) {
        if (reachableMethods.add(method)) {
            plugin.handleNewMethod(method);
            method.getNewIR().getStmts()
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

    /**
     * Process the statements in context-sensitive new reachable methods.
     */
    private class StmtProcessor implements StmtVisitor {

        private final CSMethod csMethod;

        private final Context context;

        StmtProcessor(CSMethod csMethod) {
            this.csMethod = csMethod;
            this.context = csMethod.getContext();
        }

        @Override
        public void visit(New stmt) {
            // obtain context-sensitive heap object
            Obj obj = heapModel.getObj(stmt.getRValue());
            Context heapContext = contextSelector.selectHeapContext(csMethod, obj);
            addPointsTo(context, stmt.getLValue(), heapContext, obj);
            // TODO: handle new multi-array
        }

        @Override
        public void visit(AssignLiteral stmt) {
            Literal literal = stmt.getRValue();
            if (!(literal instanceof ReferenceLiteral) ||
                    literal instanceof NullLiteral) {
                return;
            }
            Obj obj = heapModel.getConstantObj(literal.getType(),
                    ((ReferenceLiteral<?>) literal).getValue());
            Context heapContext = contextSelector.selectHeapContext(csMethod, obj);
            addPointsTo(context, stmt.getLValue(), heapContext, obj);
        }

        @Override
        public void visit(Copy stmt) {
            CSVar from = csManager.getCSVar(context, stmt.getRValue());
            CSVar to = csManager.getCSVar(context, stmt.getLValue());
            addPFGEdge(from, to, PointerFlowEdge.Kind.LOCAL_ASSIGN);
        }

        @Override
        public void visit(Cast stmt) {
            CastExp cast = stmt.getRValue();
            CSVar from = csManager.getCSVar(context, cast.getValue());
            CSVar to = csManager.getCSVar(context, stmt.getLValue());
            addPFGEdge(from, to, cast.getType(), PointerFlowEdge.Kind.CAST);
        }

        @Override
        public void visit(LoadField stmt) {
            FieldAccess fieldAccess = stmt.getRValue();
            if (fieldAccess instanceof StaticFieldAccess) {
                JField field = fieldAccess.getFieldRef().resolve();
                StaticField sfield = csManager.getStaticField(field);
                CSVar to = csManager.getCSVar(context, stmt.getLValue());
                addPFGEdge(sfield, to, PointerFlowEdge.Kind.STATIC_LOAD);
            }
        }

        @Override
        public void visit(StoreField stmt) {
            FieldAccess fieldAccess = stmt.getLValue();
            if (fieldAccess instanceof StaticFieldAccess) {
                JField field = fieldAccess.getFieldRef().resolve();
                StaticField sfield = csManager.getStaticField(field);
                CSVar from = csManager.getCSVar(context, stmt.getRValue());
                addPFGEdge(from, sfield, PointerFlowEdge.Kind.STATIC_STORE);
            }
        }

        @Override
        public void visit(Invoke stmt) {
            InvokeExp callSite = stmt.getInvokeExp();
            if (callSite instanceof InvokeStatic) {
                JMethod callee = resolveCallee(null, callSite);
                CSCallSite csCallSite = csManager.getCSCallSite(context, callSite);
                Context calleeCtx = contextSelector.selectContext(csCallSite, callee);
                CSMethod csCallee = csManager.getCSMethod(calleeCtx, callee);
                Edge<CSCallSite, CSMethod> edge =
                        new Edge<>(CallKind.STATIC, csCallSite, csCallee);
                workList.addCallEdge(edge);
            }
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
            if (rvalue.getType() instanceof ReferenceType) {
                initializeClass(extractClass(rvalue.getType()));
                if (rvalue instanceof ClassLiteral) {
                    initializeClass(extractClass(
                            ((ClassLiteral) rvalue).getValue()));
                }
            }
        }

        /**
         * Analyzes the initializer of given class.
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
            processMemberRef(stmt.getInvokeExp().getMethodRef());
        }

        @Override
        public void visit(LoadField stmt) {
            processMemberRef(stmt.getRValue().getFieldRef());
        }

        @Override
        public void visit(StoreField stmt) {
            processMemberRef(stmt.getLValue().getFieldRef());
        }

        private void processMemberRef(MemberRef memberRef) {
            if (memberRef.isStatic()) {
                initializeClass(memberRef.resolve().getDeclaringClass());
            }
        }
    }
}
