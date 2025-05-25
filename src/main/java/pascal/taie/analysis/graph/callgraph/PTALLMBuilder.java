package pascal.taie.analysis.graph.callgraph;

import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResultImpl;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultExImpl;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.*;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.apache.logging.log4j.LogManager.*;
import static pascal.taie.language.classes.ClassNames.CALL_SITE;
import static pascal.taie.language.classes.ClassNames.METHOD_HANDLE;

/**
 * Builds call graph based on pointer analysis results.
 * This builder assumes that pointer analysis has finished,
 * but it does not merely return the (context-insensitive) call graph.
 * Instead, it conducts LLM base CFA and obtain a new call graph
 * which excludes calls on unreachable branches
 */
public class PTALLMBuilder implements CGBuilder<Invoke, JMethod> {

    private static final Logger logger = getLogger(PTALLMBuilder.class);

    private ClassHierarchy hierarchy;

    /**
     * SubSignatures of methods in java.lang.Object.
     */
    private Set<Subsignature> objectMethods;

    /**
     * Cache resolve results for interface/virtual invocations.
     */
    private TwoKeyMap<JClass, MemberRef, Set<JMethod>> resolveTable;

    /**
     * Store full pta result for CSCallGraph Construction
     * result.getBase() returns a PointerAnalysisResult instance
     */
    private PointerAnalysisResultExImpl exPtaResult;

    /**
     * ptaResult.getPointsToSet(v) check object of a variable
     */
    private PointerAnalysisResultImpl ptaResult;

    /**
     * record for the second work list, each contains method and its pre-conditions
     * @param constraints represent pre-conditions when entering method
     * @param method JMethod
     */
    private record Entry(List<List<String>> constraints, JMethod method) {}

    /**
     * record for argument range LLM queries
     * @param arg args of invoke
     * @param range constraints of an arg
     */
    private record ArgRange(Var arg, List<String> range){}

    /**
     * record for param range recording. A method maps to a list of ParamRanges
     * @param paramName param name
     * @param paramType param type
     * @param range constraints of a param
     */
    private record ParamRange(String paramName, Type paramType, List<String> range){
        public ParamRange(ParamRange a, ParamRange b){
            // merge two ParamRanges
            this(a.paramName,a.paramType,Objects.equals(a.paramName, b.paramName)
                    && Objects.equals(a.paramType, b.paramType) ? Stream
                    .concat(a.range.stream(), b.range.stream())
                    .distinct()
                    .toList() : List.of("error"));
            if(Objects.equals(this.range, List.of("error"))){
                logger.error("unmatched param type and name");
                throw new AnalysisException("params unmatched: "+
                        a.paramType.getName()+" "+a.paramName+" and "
                        +b.paramType.getName()+" "+b.paramName);
            }
        }
    }

    private Map<JMethod, List<ParamRange>> methodParamRange;

    @Override
    public CallGraph<Invoke, JMethod> build() {
        hierarchy = World.get().getClassHierarchy();
        JClass object = hierarchy.getJREClass(ClassNames.OBJECT);
        objectMethods = Objects.requireNonNull(object)
                .getDeclaredMethods()
                .stream()
                .map(JMethod::getSubsignature)
                .collect(Collectors.toUnmodifiableSet());
        resolveTable = Maps.newTwoKeyMap();
        /*
          following getResult returns a PointerAnalysisResultImpl instance
          prove it in this path:
          DefaultSolver.getResult() ->
          PointerAnalysis.runAnalysis() -> analyze() ->
          AnalysisManager.runProgramAnalysis() ->
          AbstractResultHolder.storeResult() -> getResult()
         */
        ptaResult = World.get().getResult(PointerAnalysis.ID);
        exPtaResult = new PointerAnalysisResultExImpl(ptaResult,true);
        // first round of work list
        methodParamRange = buildRange(World.get().getMainMethod());
        // second round
        return buildCallGraph(World.get().getMainMethod());
    }

    private Map<JMethod, List<ParamRange>> buildRange(JMethod entry) {
        logger.info("resolving param range method by method...");

        Map<JMethod, List<ParamRange>> paramRanges = new HashMap<>();

        // call graph only for recording and util functions
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryMethod(entry);
        Queue<JMethod> workList = new ArrayDeque<>();
        workList.add(entry);
        while (!workList.isEmpty()) {
            JMethod method = workList.poll();
            if (callGraph.addReachableMethod(method)) {
                //construct llm queries for each method
                final Map<Invoke, List<ArgRange>> queries = new HashMap<>();
                callGraph.callSitesIn(method).forEach(invoke -> {
                    // params of invoke
                    List<ArgRange> params = new ArrayList<>();
                    invoke.getInvokeExp().getArgs().forEach(arg -> {
                        ArgRange argRange = new ArgRange(arg, new ArrayList<>());
                        params.add(argRange);
                    });
                    queries.put(invoke, params);
                });
                // now choose ask answers in one go

                // change to get and deal with result in inter const prop?
                final Map<Invoke, List<ArgRange>> answers = LLMQuery(queries);

                // update answers
                answers.forEach((invoke, argRanges) -> {
                    // !!!!IMPORTANT!!!! this function contains analysis in pta
                    // and it might be wrong
                    Set<JMethod> callees = resolveCalleesOf(invoke);
                    if (callees != null) {
                        // only analyse application methods
                        // seems only-app way may got wrong. commented filter
                        callees // .stream()
                                // .filter(callee -> !isIgnored(callee))
                                .forEach(callee -> {
                            if (!callGraph.contains(callee)) {
                                workList.add(callee);
                            }
                            // make sure invoke params matches with method params.
                            if (!fieldsEqual(callee.getParamTypes(),
                                    invoke.getInvokeExp().getArgs())){
                                logAndThrow(invoke, callee);
                            }
                            // now: invoke args -> callee params
                            // ranges: the param ranges of this callee
                            // argRange -> paramRange. Done
                            List<Type> types = callee.getParamTypes();
                            List<ParamRange> ranges = IntStream.range(0, callee.getParamCount())
                                    .mapToObj(i -> new ParamRange(callee.getParamName(i),
                                            types.get(i), argRanges.get(i).range))
                                    .collect(Collectors.toList());
                            // add ranges to the final result paramRanges
                            paramRanges.compute(callee, (k, currentRanges) -> {
                                if (currentRanges == null) {
                                    // return new param ranges
                                    return ranges;
                                }
                                else{
                                    return appendRange(callee,currentRanges,ranges);
                                }
                            });
                            callGraph.addEdge(new Edge<>(
                                    CallGraphs.getCallKind(invoke), invoke, callee));
                        });
                    }
                    else {
                        logger.error("Failed to resolve {}, Invoke {} cannot find callee.",
                                invoke.getInvokeExp().getMethodRef(),invoke.toString());
                    }
                });
            }
        }
        return paramRanges;
    }

    private void logAndThrow(JMethod callee) throws AnalysisException {
        logger.error("unmatched param number");
        throw new AnalysisException("paramRanges unmatched."+
                callee.getDeclaringClass().getName()+"."
                +callee.getName());
    }

    private void logAndThrow(Invoke invoke, JMethod callee) throws AnalysisException {
        logger.error("unmatched method");
        throw new AnalysisException(invoke + " mismatch to "
                +callee.getDeclaringClass().getName()+"."+
                callee.getName()+", params unmatched.");

    }

    private List<ParamRange> appendRange(
            JMethod callee, List<ParamRange> currentRanges, List<ParamRange> ranges) {
        if (currentRanges.size()!=ranges.size()) {
            logAndThrow(callee);
        }
        // return merged param ranges
        return IntStream
                .range(0, ranges.size())
                .mapToObj(i -> new ParamRange(ranges.get(i),
                        currentRanges.get(i)))
                .toList();
    }

    private static boolean fieldsEqual(List<Type> listA, List<Var> listB) {
        List<Type> listAFromB = listB.stream()
                .map(Var::getType)
                .toList();
        return listA.equals(listAFromB);
    }

    private boolean isIgnored(JMethod method) {
        return !method.isApplication();
    }

    private Map<Invoke, List<ArgRange>> LLMQuery(Map<Invoke, List<ArgRange>> queries) {
        // TODO: implement LLMQuery, may use MCP Java / autogen
        return queries;
    }

    private CallGraph<Invoke, JMethod> buildCallGraph(JMethod entry) {
        logger.info("Building Call Graph with LLM agent...");
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryMethod(entry);
        Queue<Entry> workList = new ArrayDeque<>();
        Entry mainEntry = new Entry(new ArrayList<>(entry.getParamCount()), entry);
        workList.add(mainEntry);
        while (!workList.isEmpty()) {
            Entry invoke = workList.poll();
            JMethod invokeMethod = invoke.method();
            // TODO: how to use methodParamRange
            if (callGraph.addReachableMethod(invokeMethod)) {
                IR ir = invokeMethod.getIR();

                // TODO: checkout which invokes are not reachable with LLM / CE
                // Step 1: find out all branches in cfg
                // Step 2: write a interface for concolic execution and LLM(get information)
                // Step 3: handle interface with z3 solver

//                callGraph.callSitesIn(method).forEach(invoke -> {
//                    Set<JMethod> callees = resolveCalleesOf(invoke);
//                    callees.forEach(callee -> {
//                        if (!callGraph.contains(callee)) {
//                            workList.add(callee);
//                        }
//                        callGraph.addEdge(new Edge<>(
//                                CallGraphs.getCallKind(invoke), invoke, callee));
//                    });
//                });
            }
        }
        return callGraph;
    }

    /**
     * Resolves callees of a call site via pta results.
     */
    @Nullable
    private Set<JMethod> resolveCalleesOf(Invoke callSite) {
        CallKind kind = CallGraphs.getCallKind(callSite);
        return switch (kind) {
            case INTERFACE, SPECIAL, VIRTUAL -> {
                MethodRef methodRef = callSite.getMethodRef();
                if (isObjectMethod(methodRef)) {
                    yield Set.of();
                }
                Var base = ((InvokeInstanceExp)callSite.getInvokeExp()).getBase();
                //resolve callee via pta result
                yield ptaResult.getPointsToSet(base).stream()
                        .map(recvObj -> {
                            JMethod callee = CallGraphs.resolveCallee(
                                    recvObj.getType(), callSite);
                            return callee == null ? handleLambda(callSite) : callee;
                        })
                        .collect(Collectors.toSet());
            }
            case STATIC -> {
                JMethod callee = CallGraphs.resolveCallee(null, callSite);
                if (callee!= null) yield Set.of(callee);
                else yield Set.of();
            }
            case DYNAMIC -> {
                Set<JMethod> callees = new HashSet<>();
                if (isBSMInvoke(callSite)) {
                    // ignore lambda functions
                    // will be handled by further code
                    InvokeDynamic indyBSM = (InvokeDynamic) callSite.getInvokeExp();
                    JMethod bsm = indyBSM.getBootstrapMethodRef().resolve();
                    callees.add(bsm);
                    bsm.getIR()
                            .invokes(true)
                            .map(Invoke::getInvokeExp)
                            .map(this::getMhVar)
                            .filter(Objects::nonNull)
                            .forEach(mhVar -> ptaResult.getPointsToSet(mhVar).forEach(recvObj -> {
                                MethodHandle mh = recvObj.getAllocation()
                                        instanceof MethodHandle methodHandle ?
                                        methodHandle : null;
                                if (mh != null) {
                                    MethodRef ref = mh.getMethodRef();
                                    switch (mh.getKind()) {
                                        case REF_invokeVirtual -> {
                                            // for virtual invocation, record base variable and
                                            // add invokedynamic call edge
                                            Var base = callSite.getInvokeExp().getArg(0);
                                            Set<Obj> recvObjs = ptaResult.getPointsToSet(
                                                    base);
                                            recvObjs.forEach(recv ->{
                                                JMethod callee =
                                                        hierarchy.dispatch(recv.getType(), ref);
                                                if (callee != null) {
                                                    callees.add(callee);
                                                }
                                            });
                                        }
                                        case REF_invokeStatic ->
                                            // for static invocation, just add invokedynamic call edge
                                                callees.add(ref.resolve());
                                    }
                                }
                            }));
                }
                else { // deal with lambda
                    callees.add(handleLambda(callSite));
                }
                yield callees;
            }
            default -> throw new AnalysisException(
                    "Failed to resolve call site: " + callSite);
        };
    }

    @Nullable
    private JMethod handleLambda(Invoke callSite) {
        AtomicReference<JMethod> callee = new AtomicReference<>();
        Var base = ((InvokeInstanceExp) callSite.getInvokeExp()).getBase();
        Set<Obj> objsOfIndy = ptaResult.getPointsToSet(base);
        Descriptor LAMBDA_DESC = () -> "LambdaObj";
        Descriptor LAMBDA_NEW_DESC = () -> "LambdaConstructedObj";
        for (Obj obj : objsOfIndy) {
            // for each obj, try to find a callee and add into callees
            if (obj instanceof MockObj mockObj &&
                    mockObj.getDescriptor().equals(LAMBDA_DESC)) {
                Invoke indyInvoke = (Invoke) mockObj.getAllocation();
                InvokeDynamic indyLambda = (InvokeDynamic) indyInvoke.getInvokeExp();
                if (indyLambda.getMethodName()
                        .equals(callSite.getMethodRef().getName())) {
                    // lines of original LambdaAnalysis
                    MethodHandle mh = (MethodHandle) indyLambda.getBootstrapArgs().get(1);
                    final MethodRef targetRef = mh.getMethodRef();
                    switch (mh.getKind()) {
                        case REF_newInvokeSpecial -> { // targetRef is constructor
                                        /*
                                         Create mock object (if absent) which represents
                                         the newly-allocated object. Note that here we use the
                                         *invokedynamic* to represent the *allocation site*,
                                         instead of the actual invocation site of the constructor.
                                        */
                            ClassType type = targetRef.getDeclaringClass().getType();
                            Obj newObj = new MockObj(LAMBDA_NEW_DESC,
                                    indyInvoke, type, indyInvoke.getContainer(),
                                    true);
                            // add call edge to constructor
                            // recvObj is not null, meaning that callee is instance method
                            callee.set(hierarchy.dispatch(newObj.getType(),
                                    targetRef));
                        }
                        case REF_invokeInterface, REF_invokeVirtual,
                             REF_invokeSpecial -> { // targetRef is instance method
                            Var recvVar = getLambdaRecvVar(callSite, indyLambda);
                            ptaResult.getPointsToSet(recvVar).forEach(recvObj -> {
                                if (recvObj != null) {
                                    // recvObj is not null, meaning that
                                    // callee is instance method
                                    callee.set(hierarchy.dispatch(recvObj.getType(),
                                            targetRef));
                                } else { // otherwise, callee is static method
                                    callee.set(targetRef.resolveNullable());
                                }
                            });
                        }
                        case REF_invokeStatic -> // targetRef is static method
                                callee.set(targetRef.resolveNullable());
                        default -> {
                            logger.error("{} is not supported by Lambda handling",
                                    mh.getKind());
                            throw new AnalysisException(mh.getKind() + " is not supported");
                        }
                    }
                }
            }
        }
        return callee.get();
    }

    private Var getMhVar(InvokeExp ie) {
        MethodRef ref = ie.getMethodRef();
        ClassType declType = ref.getDeclaringClass().getType();
        if (World.get().getTypeSystem()
                .isSubtype(requireNonNull(hierarchy.getJREClass(CALL_SITE))
                        .getType(), declType)) {
            // new [Constant|Mutable|Volatile]CallSite(target);
            if (ref.getName().equals(MethodNames.INIT) ||
                    // callSite.setTarget(target);
                    ref.getName().equals("setTarget")) {
                Var tgt = ie.getArg(0);
                if (tgt.getType().equals(requireNonNull(
                        hierarchy.getJREClass(METHOD_HANDLE)).getType())) {
                    return tgt;
                }
            }
        }
        return null;
    }

    private static Var getLambdaRecvVar(Invoke callSite, InvokeDynamic indyLambda) {
        List<Var> capturedArgs = indyLambda.getArgs();
        List<Var> actualArgs = callSite.getInvokeExp().getArgs();
        // if captured arguments are not empty, then the first one
        // must be the receiver object for targetRef
        // otherwise, the first actual argument is the receiver
        return !capturedArgs.isEmpty() ? capturedArgs.get(0)
                : actualArgs.get(0);
    }

    private boolean isBSMInvoke(Invoke invoke){
        boolean isLambda;
        boolean isJava9;
        // originally from Lambda and Java9StringConcat class
        JMethod bsm = ((InvokeDynamic) invoke.getInvokeExp())
                .getBootstrapMethodRef()
                .resolveNullable();
        if (bsm != null) {
            String bsmSig = bsm.getSignature();
            isLambda = bsmSig.equals(Signatures.LAMBDA_METAFACTORY) ||
                    bsmSig.equals(Signatures.LAMBDA_ALTMETAFACTORY);
            isJava9 = bsmSig.equals(Signatures.STRING_CONCAT_FACTORY_MAKE);
        } else {
            isLambda = false;
            isJava9 = false;
        }
        return !isLambda&&!isJava9;
    }

    private boolean isObjectMethod(MethodRef methodRef) {
        return objectMethods.contains(methodRef.getSubsignature());
    }

}
