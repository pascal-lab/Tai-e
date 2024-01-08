/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static pascal.taie.language.classes.ClassNames.CALL_SITE;
import static pascal.taie.language.classes.ClassNames.LOOKUP;
import static pascal.taie.language.classes.ClassNames.METHOD_HANDLE;

public class InvokeDynamicAnalysis implements Plugin {

    private Solver solver;

    private CSManager csManager;

    private ContextSelector selector;

    private HeapModel heapModel;

    private ClassHierarchy hierarchy;

    private TypeSystem typeSystem;

    private Context defContext;

    /**
     * Class type java.lang.invoke.MethodHandles$Lookup
     */
    private ClassType lookup;

    /**
     * Class type java.lang.invoke.MethodHandle
     */
    private ClassType methodHandle;

    /**
     * Class type java.lang.invoke.CallSite
     */
    private ClassType callSite;

    private Plugin methodTypeModel;

    private Plugin lookupModel;

    /**
     * Map from method to the invokedynamic invocations included in the method.
     * Updated in {@link #onNewMethod}.
     */
    private final MultiMap<JMethod, Invoke> method2indys = Maps.newMultiMap();

    /**
     * Map from method (containing invokedynamic) to its context-sensitive methods.
     * Updated in {@link #onNewCSMethod}.
     */
    private final MultiMap<JMethod, Context> method2ctxs = Maps.newMultiMap();

    /**
     * Map from variable that holds the MethodHandle to the corresponding
     * invokedynamic invocation site.
     * Updated in {@link #onNewMethod}.
     */
    private final MultiMap<Var, Invoke> mhVar2indys = Maps.newMultiMap();

    /**
     * Map from invokedynamic invocation site to the corresponding
     * MethodHandle bound to it.
     * Updated in {@link #onNewPointsToSet}.
     */
    private final MultiMap<Invoke, MethodHandle> indy2mhs = Maps.newMultiMap();

    /**
     * Map from base variable (arg0) to the corresponding invokedynamic
     * invocation sites.
     * Updated in {@link #onNewPointsToSet}
     */
    private final MultiMap<Var, Invoke> base2Indys = Maps.newMultiMap();

    /**
     * Description for MethodHandles.Lookup objects.
     */
    private static final Descriptor LOOKUP_DESC = () -> "MethodHandlesLookupObj";

    /**
     * Map from class type to corresponding Method.Lookup object.
     */
    private final Map<ClassType, Obj> lookupObjs = Maps.newMap();

    /**
     * @return true if java.lang.invoke.MethodHandle is used by
     * the program being analyzed, otherwise false.
     */
    public static boolean useMethodHandle() {
        // if MethodHandle is not loaded, we consider it as unused.
        return World.get().getClassHierarchy()
                .getJREClass(METHOD_HANDLE) != null;
    }

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        csManager = solver.getCSManager();
        selector = solver.getContextSelector();
        heapModel = solver.getHeapModel();
        hierarchy = solver.getHierarchy();
        typeSystem = solver.getTypeSystem();

        defContext = selector.getEmptyContext();
        lookup = requireNonNull(hierarchy.getJREClass(LOOKUP)).getType();
        methodHandle = requireNonNull(hierarchy.getJREClass(METHOD_HANDLE)).getType();
        callSite = requireNonNull(hierarchy.getJREClass(CALL_SITE)).getType();
        // TODO: add option to enable MethodTypeModel
        methodTypeModel = Plugin.DUMMY;
        lookupModel = new LookupModel(solver);
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {

        if (stmt instanceof Invoke invoke) {
            if (!invoke.isDynamic()) {
                methodTypeModel.onNewStmt(stmt, container);
                lookupModel.onNewStmt(stmt, container);
            }
            InvokeDynamic indy = getInvokeDynamic(invoke);
            if (indy != null) {
                // if new reachable method contains invokedynamic,
                // then we record necessary information
                method2indys.put(container, invoke);
                JMethod bsm = indy.getBootstrapMethodRef().resolve();
                // we associate the variables in bootstrap method to
                // the invokedynamic, where the variables may point to
                // the MethodHandle for the invokedynamic,
                // so that when MethodHandle objects reach these variables,
                // we can associate them to the invokedynamic.
                extractMHVars(bsm).forEach(mhVar ->
                        mhVar2indys.put(mhVar, invoke));
                // add call edge to BSM
                addBSMCallEdge(invoke, bsm);
            }
        }
    }

    @Nullable
    private static InvokeDynamic getInvokeDynamic(Invoke invoke) {
        InvokeExp invokeExp = invoke.getInvokeExp();
        if (invokeExp instanceof InvokeDynamic) {
            if (!LambdaAnalysis.isLambdaMetaFactory(invoke) &&
                    !Java9StringConcatHandler.isStringConcatFactoryMake(invoke)) {
                // ignore lambda functions and string concat which
                // will be handled by specific plugins
                return (InvokeDynamic) invokeExp;
            }
        }
        return null;
    }

    /**
     * Extracts the variables that may point to MethodHandle objects for
     * the invokedynamic. We identify variables which are the first arguments
     * of new *CallSite(target) or callSite.setTarget(target).
     * Note that this is expedient (and unsound) solution. Ideally,
     * we should associate instance field CallSite.target to the invokedynamic,
     * but currently the pointer analysis does not support handling
     * on new points-to set of field (only support variable),
     * thus we connect variables to invokedynamic.
     */
    private Stream<Var> extractMHVars(JMethod bsm) {
        return bsm.getIR()
                .invokes(true)
                .map(Invoke::getInvokeExp)
                .map(ie -> {
                    MethodRef ref = ie.getMethodRef();
                    ClassType declType = ref.getDeclaringClass().getType();
                    if (typeSystem.isSubtype(callSite, declType)) {
                        // new [Constant|Mutable|Volatile]CallSite(target);
                        if (ref.getName().equals(MethodNames.INIT) ||
                                // callSite.setTarget(target);
                                ref.getName().equals("setTarget")) {
                            Var tgt = ie.getArg(0);
                            if (tgt.getType().equals(methodHandle)) {
                                return tgt;
                            }
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    private void addBSMCallEdge(Invoke invoke, JMethod bsm) {
        // each invokedynamic call site will invoke BSM at most once,
        // thus ideally we should use 1-call-site sensitivity for BSM.
        // TODO: use 1-call-site sensitivity for BSM
        BSMCallEdge edge = new BSMCallEdge(
                csManager.getCSCallSite(defContext, invoke),
                csManager.getCSMethod(defContext, bsm));
        solver.addCallEdge(edge);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof BSMCallEdge) {
            // pass arguments to boostrap method, for details, please refer to
            // https://docs.oracle.com/javase/7/docs/api/java/lang/invoke/package-summary.html
            Invoke invoke = edge.getCallSite().getCallSite();
            InvokeDynamic indy = (InvokeDynamic) invoke.getInvokeExp();
            Context context = edge.getCallee().getContext();
            IR ir = edge.getCallee().getMethod().getIR();
            // arg 0: MethodHandles.Lookup object
            solver.addVarPointsTo(context, ir.getParam(0), defContext,
                    getLookupObj(invoke));
            // arg 1: method name
            solver.addVarPointsTo(context, ir.getParam(1), defContext,
                    heapModel.getConstantObj(StringLiteral.get(indy.getMethodName())));
            // arg 2: MethodType object
            solver.addVarPointsTo(context, ir.getParam(2), defContext,
                    heapModel.getConstantObj(indy.getMethodType()));
            // arg 3+: optionally, additional static arguments
            for (int i = 0, j = 3;
                 i < indy.getBootstrapArgs().size() && j < ir.getParams().size();
                 ++i, ++j) {
                Literal arg = indy.getBootstrapArgs().get(i);
                if (arg instanceof ReferenceLiteral argLiteral) {
                    Obj argObj = heapModel.getConstantObj(argLiteral);
                    solver.addVarPointsTo(context, ir.getParam(j), defContext, argObj);
                }
            }
            // TODO: mock array for varargs
        }
        if (edge instanceof InvokeDynamicCallEdge) {
            JMethod callee = edge.getCallee().getMethod();
            // pass arguments
            int shift;
            if (callee.isStatic() || callee.isConstructor()) {
                shift = 0;
            } else { // if callee is instance method, then the first argument
                // is the receiver object, which has been passed to callee's
                // this variable when adding this call edge.
                shift = 1;
                // TODO: consider case of inner class, where shift = 2
            }
            Context callerCtx = edge.getCallSite().getContext();
            Invoke caller = edge.getCallSite().getCallSite();
            List<Var> args = caller.getInvokeExp().getArgs();
            Context calleeCtx = edge.getCallee().getContext();
            List<Var> params = callee.getIR().getParams();
            for (int i = shift, j = 0;
                 i < args.size() && j < params.size(); ++i, ++j) {
                // TODO: filter unconcerned pointers
                solver.addPFGEdge(
                        csManager.getCSVar(callerCtx, args.get(i)),
                        csManager.getCSVar(calleeCtx, params.get(j)),
                        FlowKind.PARAMETER_PASSING);
            }
            // pass return values
            Var result = caller.getResult();
            if (result != null) { // TODO: filter unconcerned pointers
                CSVar csResult = csManager.getCSVar(callerCtx, result);
                callee.getIR().getReturnVars().forEach(ret -> {
                    CSVar csRet = csManager.getCSVar(calleeCtx, ret);
                    solver.addPFGEdge(csRet, csResult, FlowKind.RETURN);
                });
            }
        }
    }

    /**
     * @return the MethodHandles.Lookup object for given invokedynamic.
     * Each Lookup object is associate with a lookup class which contains the
     * invokedynamic invocation site, and will be used for access checking.
     */
    private Obj getLookupObj(Invoke invoke) {
        ClassType type = invoke.getContainer().getDeclaringClass().getType();
        return lookupObjs.computeIfAbsent(type,
                t -> heapModel.getMockObj(LOOKUP_DESC, t, lookup));
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        methodTypeModel.onNewPointsToSet(csVar, pts);
        lookupModel.onNewPointsToSet(csVar, pts);
        Var var = csVar.getVar();
        Set<Invoke> indys = mhVar2indys.get(var);
        if (!indys.isEmpty()) {
            // if var is MethodHandle variable which was associated to
            // some invokedynamic sites, then we process new-reach
            // MethodHandle objects.
            pts.forEach(csObj -> {
                MethodHandle mh = CSObjs.toMethodHandle(csObj);
                if (mh != null) {
                    indys.forEach(invoke -> handleNewMethodHandle(invoke, mh));
                }
            });
        }
        Context context = csVar.getContext();
        base2Indys.get(var).forEach(indy -> {
            // if var is base variable of some invokedynamic that invokes
            // instance method, then we process new-reach receiver objects.
            indy2mhs.get(indy).forEach(mh -> pts.forEach(recv ->
                    addInvokeDynamicCallEdge(context, indy, recv, mh)));
        });
    }

    /**
     * Invoked when the analysis discovers that a new MethodHandle may be
     * associated to an invokedynamic invocation site.
     */
    private void handleNewMethodHandle(Invoke invoke, MethodHandle mh) {
        if (!indy2mhs.put(invoke, mh)) {
            return;
        }
        Set<Context> contexts = method2ctxs.get(invoke.getContainer());
        switch (mh.getKind()) {
            case REF_invokeVirtual -> {
                // for virtual invocation, record base variable and
                // add invokedynamic call edge
                Var base = invoke.getInvokeExp().getArg(0);
                base2Indys.put(base, invoke);
                contexts.forEach(ctx -> {
                    PointsToSet recvObjs = solver.getPointsToSetOf(
                            csManager.getCSVar(ctx, base));
                    recvObjs.forEach(recv ->
                            addInvokeDynamicCallEdge(ctx, invoke, recv, mh));
                });
            }
            case REF_invokeStatic ->
                    // for static invocation, just add invokedynamic call edge
                    contexts.forEach(ctx ->
                            addInvokeDynamicCallEdge(ctx, invoke, null, mh));
            // TODO: handle other MethodHandle operations
        }
    }

    /**
     * Adds new invokedynamic call edge. The callee is decided
     * by given receiver object (may be null) and MethodHandle.
     */
    private void addInvokeDynamicCallEdge(
            Context callerCtx, Invoke caller, CSObj recv, MethodHandle mh) {
        CSCallSite csCallSite = csManager.getCSCallSite(callerCtx, caller);
        MethodRef ref = mh.getMethodRef();
        JMethod callee;
        Context calleeCtx;
        switch (mh.getKind()) {
            case REF_invokeVirtual -> {
                callee = hierarchy.dispatch(recv.getObject().getType(), ref);
                if (callee == null) {
                    return;
                }
                calleeCtx = selector.selectContext(csCallSite, recv, callee);
                // pass receiver object
                solver.addVarPointsTo(calleeCtx, callee.getIR().getThis(), recv);
            }
            case REF_invokeStatic -> {
                callee = ref.resolve();
                calleeCtx = selector.selectContext(csCallSite, callee);
            }
            // TODO: handle other MethodHandle operations
            default -> throw new UnsupportedOperationException(
                    mh.getKind() + " is currently not supported");
        }
        solver.addCallEdge(new InvokeDynamicCallEdge(
                csCallSite, csManager.getCSMethod(calleeCtx, callee)));
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Set<Invoke> indys = method2indys.get(method);
        if (!indys.isEmpty()) {
            Context context = csMethod.getContext();
            method2ctxs.put(method, context);
            for (Invoke indy : indys) {
                for (MethodHandle mh : indy2mhs.get(indy)) {
                    // add new invokedynamic call edges
                    // for already-discovered MethodHandles
                    addInvokeDynamicCallEdge(context, indy, null, mh);
                }
            }
        }
    }
}
