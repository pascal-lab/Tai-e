package pascal.taie.analysis.pta.plugin.cutshortcut.container;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.CutShortcutSolver;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.cutshortcut.SpecialVariables;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ContExitCategory;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ExtendType;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.HostKind;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.*;

import java.util.Collections;
import java.util.Set;

import static pascal.taie.analysis.pta.core.solver.CutShortcutSolver.isConcerned;
import static pascal.taie.analysis.pta.plugin.cutshortcut.container.ClassAndTypeClassifier.*;
import static pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ContExitCategory.*;
import static pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.HostKind.*;
// ToDo: 其它自定义AbstractList类型，尤其是匿名内部类，实现了自定义get等方法
public class ContainerAccessHandler implements Plugin {
    private CutShortcutSolver solver;

    private CSManager csManager;

    private TypeSystem typeSystem;

    private HostManager hostManager;

    private HeapModel heapModel;

    private CallGraph<CSCallSite, CSMethod> callGraph;

    private ContainerConfig config;

    private final Set<HostKind> ContKindsInterested = Set.of(COL, MAP_KEY_SET, MAP_VALUES, MAP_ENTRY_SET);
    private final MultiMap<Pair<HostKind, CSVar>, Pair<HostKind, CSVar>> HostPropagater = Maps.newMultiMap();

    // The first key type of pointerHostListMap may not only be ''CSVar'':
    // a container field of a object obj.list (InstanceField) may also point-to container object.
    private final TwoKeyMap<Pointer, HostKind, PointsToSet> pointerHostListMap = Maps.newTwoKeyMap(); // ptsH

    // for Col-Value, Map-Value, Map-key exit calledge e: <c, l: r = v.get(..)> => <c^t, m>, add <c, v> -> <l, category>
    private final MultiMap<CSVar, Pair<Invoke, ContExitCategory>> cachedContExitValues = Maps.newMultiMap();
    // for MapEntry-GetKey(Map-Key), MapEntry-GetValue(Map-Value), Iterator-next/previous/nextElement(Col-Value) l: r = v.next(..), add <c, v> -> <l, hostkind, category>
    private final MultiMap<CSVar, Triplet<Invoke, HostKind, ContExitCategory>> cachedIterExitValues = Maps.newMultiMap();
    // \for v = new ArrayList(), l: r = v.get(0) => ArrayList.get, <c', o> \in pts(<c, v>), <c', o> => <c, l> \in hostToExit
    private final MultiMap<CSObj, CSCallSite> hostToExits = Maps.newMultiMap();

    // assist worklist method to handle container extender
    private final MultiMap<CSVar, Var> ExtenderAddedToBase = Maps.newMultiMap(); // for <c, l: v.addAll(v1)>, add v to <c, v1>
    private final MultiMap<CSVar, Var> ExtenderBaseToAdded = Maps.newMultiMap(); // for <c, l: v.addAll(v1)>, add v1 to <c, v>

    // process ArrayInitializer, l: add(v_d, v_s), where v_s is array, v_d is collection, for worklist, add a temp variable arr_l
    private final MultiMap<Var, Var> ArrayVarToVirtualArrayVar = Maps.newMultiMap(); // v_s --> arr_l
    private final MultiMap<Var, Var> CollectionVarToVirtualArrayVar = Maps.newMultiMap(); // v_d --> arr_l

    // We only model common container access. For user-defined container type like CustomArrayList, we usually ignore because they may define other entrance and exit method.
    // For example, CustomArrayList.addAnObject, getAnObject. Since we have not model it, soundness issue could occur.
    // But, there is [Entrance-Extend] method like List.addAll. If v is ArrayList, v.addAll(customArrayList). Where customArrayList may miss some src/dst variables.
    // After propagate to v, v can not be soundly modeled.
    // Hence here, taintHosts records those container objects whose type is modeled but may be influenced by unmodeled type container var like v.
    private final Set<CSObj> taintHosts = Sets.newSet();

    // Collect those var whose type is Map.Entry
    private static final Set<Var> MapEntryVar = Sets.newSet();

    public void setSolver(Solver solver) {
        if (solver instanceof CutShortcutSolver cutShortcutSolver) {
            this.solver = cutShortcutSolver;
            csManager = solver.getCSManager();
            typeSystem = solver.getTypeSystem();
            callGraph = solver.getCallGraph();
            heapModel = solver.getHeapModel();
            config = ContainerConfig.config;
            hostManager = new HostManager(csManager);
        }
        else
            throw new AnalysisException("Invalid solver!");
    }

    @Override
    public void onNewMethod(JMethod method) {
        method.getIR().forEach(stmt -> {
            if (stmt instanceof Cast castStmt) {
                Type castType = castStmt.getRValue().getCastType();
                if (castType instanceof ClassType classType && isMapEntryClass(classType.getJClass()))
                    MapEntryVar.add(castStmt.getRValue().getValue());
            }
        });
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        Context context = csMethod.getContext();
        JMethod method = csMethod.getMethod();
        method.getIR().forEach(stmt -> {
            if (stmt instanceof New newStmt) {
                Obj obj = heapModel.getObj(newStmt);
                Type objType = obj.getType();
                // called in Map.keySet()/values(), lhs = new KeySet()/values()
                if (!method.isStatic()) {
                    CSVar csThis = csManager.getCSVar(context, method.getIR().getThis());
                    CSVar csLHS = csManager.getCSVar(context, newStmt.getLValue());
                    if (config.isKeySetClass(objType))
                        HostPropagater.put(new Pair<>(HostKind.MAP, csThis), new Pair<>(MAP_KEY_SET, csLHS));
                    else if (config.isValueSetClass(objType))
                        HostPropagater.put(new Pair<>(HostKind.MAP, csThis), new Pair<>(MAP_VALUES, csLHS));
                }
            }
        });
    }

    // <c, l: r = v.k(l_a1, ...)> --> <c^t, m>
    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge.getKind() == CallKind.OTHER)
            return;
        CSCallSite csCallSite = edge.getCallSite();
        CSMethod csCallee = edge.getCallee(); // <c^t, m>
        Invoke callSite = csCallSite.getCallSite(); // l
        JMethod callee = csCallee.getMethod(); // m
        Context callSiteContext = csCallSite.getContext(); // c
        InvokeExp invokeExp = callSite.getInvokeExp(); // r = v.k(l_a1, ...)

        if (invokeExp instanceof InvokeInstanceExp instanceExp) {
            Var base = instanceExp.getBase(); // v
            CSVar csBase = csManager.getCSVar(callSiteContext, base); // <c, v>
            CSVar csThis = csManager.getCSVar(csCallee.getContext(), callee.getIR().getThis()); // <c^t, m_this>

            // propagate ptsH to callee this
            ContKindsInterested.forEach(hostKind -> {
                PointsToSet baseHostMap = pointerHostListMap.getOrDefault(csBase, hostKind, null);
                if (baseHostMap != null && !baseHostMap.isEmpty()) {
                    solver.addHostEntry(csThis, hostKind, baseHostMap);
                    HostPropagater.put(new Pair<>(hostKind, csBase), new Pair<>(hostKind, csThis));
                }
            });

            // Array Initializer
            Pair<Integer, Integer> arrayInitInfo = config.getArrayInitializer(callee);
            if (arrayInitInfo != null) {
                solver.addSelectedMethod(callee);
                processArrayInitializer(csCallSite, arrayInitInfo, instanceExp);
            }
            // callee is [Container Entrance-Append] like v.add(E)
            Set<Pair<ContExitCategory, Integer>> categoryIndexPairs = config.getEntranceAppendIndex(callee);
            if (!categoryIndexPairs.isEmpty())
                pointerHostListMap.getOrDefault(csBase, Collections.emptyMap()).forEach((hostKind, hostSet) ->
                        relateSourceToHosts(csCallSite, csCallee, categoryIndexPairs, hostSet));

            // callee is [Container Entrance-Extend] like v.addAll(list)
            Pair<ExtendType, Integer> entranceExtendIndex = config.getEntranceExtendIndex(callee);
            if (entranceExtendIndex != null) {
                solver.addSelectedMethod(callee);
                ExtendType extendType = entranceExtendIndex.first();
                Var addedContainer = instanceExp.getArg(entranceExtendIndex.second());
                processEntranceExtend(csBase, addedContainer, extendType);
            }

            // callee is [Container Exit]
            Var lhs = callSite.getLValue();
            if (lhs != null && isConcerned(lhs)) {
                // Standard container-element exit method, like l: r = v.get(i);, v is Collection/Map
                ContExitCategory exitCategory = config.getContainerExitCategory(callee);
                if (exitCategory != null) {
                    solver.addSelectedMethod(callee);
                    cachedContExitValues.put(csBase, new Pair<>(callSite, exitCategory)); // prepare for new worklist entry
                    addTargetToAllHost(csBase, exitCategory == ColValue ? COL : HostKind.MAP, csCallSite, exitCategory, true);
                }

                // MapEntry exit method, l: r = v.getKey(), v is MapEntry
//                else if (config.isMapEntryGetKeyMethod(callee)) {
//                    solver.addSelectedMethod(callee);
//                    addTargetToAllHost(csBase, MAP_ENTRY, csCallSite, MapKey, false);
//                    cachedIterExitValues.put(csBase, new Triplet<>(callSite, MAP_ENTRY, MapKey));
//                }
//                // MapEntry exit method, l: r = v.getValue(), v is MapEntry
//                else if (config.isMapEntryGetValueMethod(callee)) {
//                    solver.addSelectedMethod(callee);
//                    addTargetToAllHost(csBase, MAP_ENTRY, csCallSite, MapValue, false);
//                    cachedIterExitValues.put(csBase, new Triplet<>(callSite, MAP_ENTRY, MapValue));
//                }
                // iterator-element exit method, l: r = v.next()/previous()/nextElement(), v is iterator/enumeration
                else if (config.isIteratorExitMethods(callee)) {
                    solver.addSelectedMethod(callee);
                    /* an example, HashSet.iterator() return HashMap.KeyIterator type.
                     * vs = new HashSet() {allocate os}, is = vs.iterator(), vsl = is.next();
                     * vm = new HashMap() {allocate om}, im = vm.keyIterator(), vml = im.next();
                     * same next call, the former is os --ColValue--> vsl, later is om --KeyValue--> vml, but with same next method
                     * difference is that is => COL_ITR while im => MAP_KEY_ITR
                     */
                    addTargetToAllHost(csBase, MAP_VALUE_ITR, csCallSite, MapValue, false);
                    cachedIterExitValues.put(csBase, new Triplet<>(callSite, MAP_VALUE_ITR, MapValue));
                    addTargetToAllHost(csBase, MAP_KEY_ITR, csCallSite, MapKey, false);
                    cachedIterExitValues.put(csBase, new Triplet<>(callSite, MAP_KEY_ITR, MapKey));
                    addTargetToAllHost(csBase, COL_ITR, csCallSite, ColValue, false);
                    cachedIterExitValues.put(csBase, new Triplet<>(callSite, COL_ITR, ColValue));
                }
            }
        }
    }

    // <c, l: r = v.k(...)> --> <c^t, m>, where m is initializer of array List, l_a0/m_p0 are usually arrays of object
    private void processArrayInitializer(CSCallSite csCallSite, Pair<Integer, Integer> arrayInitInfo, InvokeInstanceExp instanceExp) {
        Var arrayVar = instanceExp.getArg(arrayInitInfo.first()), // l_ai, copied array
            collectionVar = arrayInitInfo.second() == -1 ? instanceExp.getBase(): instanceExp.getArg(arrayInitInfo.second()); // v
        if (!(arrayVar.getType() instanceof ArrayType))
            throw new AnalysisException("Not Array Type!");
        Type elementType = ((ArrayType) arrayVar.getType()).elementType();
        // virtual var for m
        Context callSiteContext = csCallSite.getContext();
        JMethod caller = csCallSite.getCallSite().getContainer();
        Var virtualArrayVar = new Var(caller,
                "virtualArrayVar[" + caller.getName() + ", line:" + csCallSite.getCallSite().getLineNumber() +"]",
                elementType, -1); // arr_arg_caller
        SpecialVariables.setVirtualVar(virtualArrayVar);
        ArrayVarToVirtualArrayVar.put(arrayVar, virtualArrayVar); // vs --> arr_l
        CollectionVarToVirtualArrayVar.put(collectionVar, virtualArrayVar); // vd --> arr_l
        CSVar csArray = csManager.getCSVar(callSiteContext, arrayVar); // <c, vs>
        CSVar csVirtualArray = csManager.getCSVar(callSiteContext, virtualArrayVar); // <c, arr_arg_caller>

        solver.getPointsToSetOf(csArray).forEach(csObj -> { // \forall <c'', os> \in pts(<c, vs>)
            ArrayIndex arrayIndex = csManager.getArrayIndex(csObj); // <c'', os[*]>
            // <c'', os[*]> --> <c, arr_l>
            solver.addPFGEdge(arrayIndex, csVirtualArray, FlowKind.VIRTUAL_ARRAY, elementType);
        });

        CSVar csCollection = csManager.getCSVar(callSiteContext, collectionVar); // <c, v>
        // \forall <c', od> \in ptsH(<c, v>), <c, arr_l> --> <c', host(od, ColValue)>
        getHostListOf(csCollection, COL).forEach(hostObj -> addSourceToHost(csVirtualArray, hostObj, ColValue));
    }

    /*
     * process c, l: r = v.k(..., v1, ...), where v and v1 are container variable, callee is extend method like List.addAll/Map.putAll
     * @params csBaseContainer: // <c, v>
     * @params csAddedContainer: // <c, l_ai>
     */
    private void processEntranceExtend(CSVar csBaseContainer, Var addedContainer, ExtendType extendType) {
        // Collection/Map.Keyset()/Map.Values() -> Collection
        CSVar csAddedContainer = csManager.getCSVar(csBaseContainer.getContext(), addedContainer);
        ExtenderAddedToBase.put(csAddedContainer, csBaseContainer.getVar());
        ExtenderBaseToAdded.put(csBaseContainer, addedContainer);

        if (extendType == ExtendType.ColToCol) {
            PointsToSet baseContainerSet = getHostListOf(csBaseContainer, COL);
            addHostSubsetRelation(baseContainerSet, getHostListOf(csAddedContainer, COL), ExtendType.ColToCol);
            addHostSubsetRelation(baseContainerSet, getHostListOf(csAddedContainer, MAP_KEY_SET), ExtendType.MapKeySetToCol);
            addHostSubsetRelation(baseContainerSet, getHostListOf(csAddedContainer, MAP_VALUES), ExtendType.MapValuesToCol);
        }
        // Map -> Map
        else if (extendType == ExtendType.MapToMap) {
            PointsToSet baseMapSet = getHostListOf(csBaseContainer, HostKind.MAP);
            addHostSubsetRelation(baseMapSet, getHostListOf(csAddedContainer, HostKind.MAP), ExtendType.MapToMap);
        }
        // Map.keySet() -> Col
        else if (extendType == ExtendType.MapKeySetToCol) {
            PointsToSet baseContainerSet = getHostListOf(csBaseContainer, COL);
            addHostSubsetRelation(baseContainerSet, getHostListOf(csAddedContainer, MAP_KEY_SET), ExtendType.MapKeySetToCol);
        }
    }

    private void addHostSubsetRelation(PointsToSet baseSet, PointsToSet addedSet, ExtendType extendType) {
        for (CSObj csAddedObj: addedSet) {
            // if added object is not modeled, then both base/added container variable should not be optimized by Cut-Shotrcut
            if (config.isUnmodeledClass(csAddedObj.getObject().getType()) || taintHosts.contains(csAddedObj)) {
                baseSet.forEach(this::taintHost);
                break;
            }
            baseSet.forEach(csBaseObj -> {
                // can not add object to empty collection type
                if (csBaseObj.getObject().getType().getName().contains("java.util.Collections$Empty"))
                    return;
                switch (extendType) {
                    // Collection -> Collection
                    case ColToCol -> {
                        CSVar csAddedHostPointer = hostManager.getHostVar(csAddedObj, ColValue),
                              csBaseHostPointer = hostManager.getHostVar(csBaseObj, ColValue);
                        solver.addPFGEdge(csAddedHostPointer, csBaseHostPointer, FlowKind.SUBSET);
                    }
                    // Map -> Map
                    case MapToMap -> {
                        CSVar csAddedMapKeyHostPointer = hostManager.getHostVar(csAddedObj, MapKey),
                                csBaseMapKeyHostPointer = hostManager.getHostVar(csBaseObj, MapKey);
                        solver.addPFGEdge(csAddedMapKeyHostPointer, csBaseMapKeyHostPointer, FlowKind.SUBSET);

                        CSVar csAddedMapValueHostPointer = hostManager.getHostVar(csAddedObj, MapValue),
                                csBaseMapValueHostPointer = hostManager.getHostVar(csBaseObj, MapValue);
                        solver.addPFGEdge(csAddedMapValueHostPointer, csBaseMapValueHostPointer, FlowKind.SUBSET);
                    }
                    // Map.keySet() -> Collection
                    case MapKeySetToCol -> {
                        CSVar csAddedMapKeyHostPointer = hostManager.getHostVar(csAddedObj, MapKey),
                                csBaseHostPointer = hostManager.getHostVar(csBaseObj, ColValue);
                        solver.addPFGEdge(csAddedMapKeyHostPointer, csBaseHostPointer, FlowKind.SUBSET);
                    }
                    // Map.values() -> Collection
                    case MapValuesToCol -> {
                        CSVar csAddedMapValueHostPointer = hostManager.getHostVar(csAddedObj, MapValue),
                                csBaseHostPointer = hostManager.getHostVar(csBaseObj, ColValue);
                        solver.addPFGEdge(csAddedMapValueHostPointer, csBaseHostPointer, FlowKind.SUBSET);
                    }
                }
            });
        }
    }

    /*
     * process when container object <ci, oi> is added into pts(<c, v>)
     * @params csVar: <c, v>
     * @params pts: {<c1, o1>, .., <cn, on>} added to pts(<c, v>)
     */
    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        PointsToSet mapSet = solver.makePointsToSet(),
                    colSet = solver.makePointsToSet();
        pts.forEach(csObj -> { // <ci, oi>
            // process when oi is container object
            Type objType = csObj.getObject().getType(); // Type(oi)
            switch (ClassificationOf(objType)) {
                case MAP -> mapSet.addObject(csObj);
                case COLLECTION -> colSet.addObject(csObj);
            }

            // if v is arguments of ArrayInitializer like ArrayList.<init>(array), add PFG edge: <ci, oi[*]> --> <c, arr_l>
            ArrayVarToVirtualArrayVar.get(csVar.getVar()).forEach(arrVar -> {
                CSVar csArrVar = csManager.getCSVar(csVar.getContext(), arrVar); // <c, arr_l>
                ArrayIndex arrayIndex = csManager.getArrayIndex(csObj); // <ci, oi[*]>
                solver.addPFGEdge(arrayIndex, csArrVar, FlowKind.VIRTUAL_ARRAY, arrVar.getType()); // <ci, oi[*]> --> <c, arr_l>
            });
        });
        // [MapHost], ptsH(<c, v>, Map).update(MapSet)
        if (!mapSet.isEmpty()) {
            getHostListOf(csVar, HostKind.MAP).addAll(mapSet);
            onNewHostEntry(csVar, mapSet, HostKind.MAP);
        }
        // [ColHost], ptsH(<c, v>, Col).update(ColSet)
        if (!colSet.isEmpty()) {
            getHostListOf(csVar, COL).addAll(colSet);
            onNewHostEntry(csVar, colSet, COL);
        }
    }

    /*
     * @params csVar: <c, v>
     * @params entrySet: {<c1, o1>, ... , <cn, on>} , new added to ptsH(<c, v>[host])
     * @params hostkind: type of host variable (MapKey, MapValue, ColValue, ColItr, etc.)
     */
    public void onNewHostEntry(CSVar csVar, PointsToSet hostSet, HostKind hostKind) {
        TransferAndMapEntryExit(csVar, hostSet, hostKind);
        ProcessCachedExitInvokes(csVar, hostSet, hostKind); // process [HostTarget], propagate ptsH to exit variables
        Context context = csVar.getContext(); // c
        Var base = csVar.getVar(); // v
        // process [HostSource]
        base.getInvokes().forEach(invoke -> {
            CSCallSite csCallSite = csManager.getCSCallSite(context, invoke); // <c, l: r = v.k(...)>
            callGraph.getCalleesOf(csCallSite).forEach(csMethod -> {
                Set<Pair<ContExitCategory, Integer>> entranceInfo = config.getEntranceAppendIndex(csMethod.getMethod());
                // propagate ptsH to this variable of container class.
                if (!entranceInfo.isEmpty())
                    relateSourceToHosts(csCallSite, csMethod, entranceInfo, hostSet);
            });
        });

        // \forall arr_l -> v, must be collection type
        if (hostKind == COL) {
            CollectionVarToVirtualArrayVar.get(base).forEach(arrVar -> {
                CSVar csArrVar = csManager.getCSVar(context, arrVar);
                hostSet.forEach(hostObj -> addSourceToHost(csArrVar, hostObj, ColValue));
            });
        }

        // \forall v.addAll(v1) cached, since {<c1, o1>, ...} added to ptsH(v), we need to update PFG xx -> <ci, host(oi)> according to ptsH(v1)
        ExtenderBaseToAdded.get(csVar).forEach(addedContainer -> { // <c, v1>
            CSVar csAddedContainer = csManager.getCSVar(csVar.getContext(), addedContainer);
            // Collection/Map.keySet()/Map.values() -> Collection, where v can only be collection
            if (hostKind == COL) {
                addHostSubsetRelation(hostSet, getHostListOf(csAddedContainer, COL), ExtendType.ColToCol);
                addHostSubsetRelation(hostSet, getHostListOf(csAddedContainer, MAP_KEY_SET), ExtendType.MapKeySetToCol);
                addHostSubsetRelation(hostSet, getHostListOf(csAddedContainer, MAP_VALUES), ExtendType.MapValuesToCol);
            }
            // Map -> Map, v can only be map
            else if (hostKind == HostKind.MAP)
                addHostSubsetRelation(hostSet, getHostListOf(csAddedContainer, HostKind.MAP), ExtendType.MapToMap);
        });

        // \forall vc.addAll(v) cached, since {<c1, o1>, ...} added to ptsH(v), we need to update PFG <ci, host(oi)> -> xx according to ptsH(vc)
        ExtenderAddedToBase.get(csVar).forEach(baseContainer -> { // <c, vc>
            CSVar csBaseContainer = csManager.getCSVar(csVar.getContext(), baseContainer);
            // Collection -> Collection
            if (hostKind == COL)
                addHostSubsetRelation(getHostListOf(csBaseContainer, COL), hostSet, ExtendType.ColToCol);
            // Map.keySet() -> Collection
            else if (hostKind == MAP_KEY_SET)
                addHostSubsetRelation(getHostListOf(csBaseContainer, COL), hostSet, ExtendType.MapKeySetToCol);
            // Collection -> Collection
            else if (hostKind == MAP_VALUES)
                addHostSubsetRelation(getHostListOf(csBaseContainer, COL), hostSet, ExtendType.MapValuesToCol);
            // Map -> Map, vc can only be map
            else if (hostKind == HostKind.MAP)
                addHostSubsetRelation(getHostListOf(csBaseContainer, HostKind.MAP), hostSet, ExtendType.MapToMap);
        });

        HostPropagater.get(new Pair<>(hostKind, csVar)).forEach(kindCSVarPair ->
                solver.addHostEntry(kindCSVarPair.second(), kindCSVarPair.first(), hostSet));
    }

    /*
     * @params csCallSite: <c, l: v.k(a)>, v is a container variable
     * @params csCallee: <c^t, m>, m is a container access method
     * @params categoryWithindex: {<category, i>, ...}. For example, Map.put(k, v) -> {<MapKey, 0>, <MapValue, 1>}
     * @params hostSet: ptsH(<c, v>) => {<c1, o1>, ... , <cn, on>}
     */
    private void relateSourceToHosts(CSCallSite csCallSite, CSMethod csCallee, Set<Pair<ContExitCategory, Integer>> categoryIndexPairs, PointsToSet hostSet) {
        JMethod callee = csCallee.getMethod(); // m
        ClassType classType = callee.getDeclaringClass().getType(); // container class type
        solver.addSelectedMethod(callee);
        for (Pair<ContExitCategory, Integer> categoryIndexPair: categoryIndexPairs) {
            Var argument = csCallSite.getCallSite().getInvokeExp().getArg(categoryIndexPair.second()); // source argument, l_ai
            CSVar csArg = csManager.getCSVar(csCallSite.getContext(), argument); // <c, l_ai>
            // filter: csObj must be subtype of container class type can be transfer to this variable
            // do: generate source relation: <c, l_ai> <--category-- <ci, oi>
            hostSet.getObjects().stream().filter(csObj -> typeSystem.isSubtype(classType, csObj.getObject().getType()))
                    .forEach(csObj -> addSourceToHost(csArg, csObj, categoryIndexPair.first()));
        }

    }

    /*
     * [TransferHost]
     * @params csVar: <c, v>
     * @params containerType: type of container (Map or Collection)
     * @params hostSet: {<c1, o1>, ... , <cn, on>} , new added to ptsH(<c, v>[containerType])
     */
    private void TransferAndMapEntryExit(CSVar csVar, PointsToSet hostSet, HostKind hostKind) {
        Var varBase = csVar.getVar(); // container variable v
        Context context = csVar.getContext(); // c
        varBase.getInvokes().forEach(invoke -> { // l: r = v.k(...)
            Var lhs = invoke.getLValue(); // r
            if (lhs == null || !isConcerned(lhs))
                return;
            InvokeExp invokeExp = invoke.getInvokeExp(); // r = v.k(...)
            String invokeString = invokeExp.getMethodRef().getName();
            CSVar csLHS = csManager.getCSVar(context, lhs); // <c, r>

            // Transfer Method: List/Set.iterator, Map.entrySet, Map.keySet, Map.values, MapEntry,iterator, MapEntry.next, etc.
            config.getTransferAPIs().forEach(((hostKindOri, methodStr, hostKindGen) -> {
                if (hostKind == hostKindOri && invokeString.contains(methodStr))
                    solver.addHostEntry(csLHS, hostKindGen, hostSet);
            }));

            // Transfer Method: Vector.elements(), Hashtable.elements()
            switch (hostKind) {
                // Vector.elements() --> col_itr，注意其它list type没有elements方法
                case COL -> {
                    if (invokeString.equals("elements") && isVectorType(varBase.getType()))
                        solver.addHostEntry(csLHS, COL_ITR, hostSet);
                }
                // Hashtable.elements() --> map_value_itr, 注意其它map type没有elements方法
                case MAP -> {
                    if ((invokeString.equals("elements") && isHashtableType(varBase.getType())))
                        solver.addHostEntry(csLHS, MAP_VALUE_ITR, hostSet);
                }
            }

            // Map-Entry Exit: map.entry.getKey(), map.entry.getValue()
            config.getMapEntryExits().forEach((hostKindOri, methodStr, category) -> {
                if (hostKind == hostKindOri && invokeString.contains(methodStr))
                    hostSet.forEach(host ->
                            checkHostRelatedExit(csManager.getCSCallSite(context, invoke), host, category));
            });
        });
    }

    // process related target invoke: l: r = v.k() for <c, v>
    private void ProcessCachedExitInvokes(CSVar csVar, PointsToSet hostSet, HostKind hostKind) {
        // container-exit like: r = v.get(..)
        cachedContExitValues.get(csVar).forEach(invokeCategoryPair -> { // out invokes
            Invoke callSite = invokeCategoryPair.first(); // l: r = v.k(...)
            hostSet.forEach(host -> {
                if (typeSystem.isSubtype(csVar.getVar().getType(), host.getObject().getType()))
                    checkHostRelatedExit(csManager.getCSCallSite(csVar.getContext(), callSite), host, invokeCategoryPair.second());
            });
        });

        // iterator-exit like: r = v.next(), here v is Iterator type, not Container type. Hence do not check type with typeSystem.isSubtype
        cachedIterExitValues.get(csVar).forEach(invokeTrip -> {
            // v could be COL_ITR/MAP_KEY_ITR/MAP_VALUE_ITR, so first we need to match
            if (hostKind != invokeTrip.second())
                return;
            Invoke callSite = invokeTrip.first(); // l: r = v.k(...)>
            hostSet.forEach(host ->
                    checkHostRelatedExit(csManager.getCSCallSite(csVar.getContext(), callSite), host, invokeTrip.third()));
        });
    }

    // for exit calledge e: <c, l: r = v.k()> => <c^t, m>, \forall <c', o> \in ptsH(<c, v>[hostKind]), add <c', o> --exitCategory--> <c, r>
    private void addTargetToAllHost(CSVar csBase, HostKind hostKind, CSCallSite csCallSite, ContExitCategory exitCategory, boolean checkType) {
        getHostListOf(csBase, hostKind).forEach(csObj -> {
            if (!checkType || typeSystem.isSubtype(csBase.getType(), csObj.getObject().getType()))
                checkHostRelatedExit(csCallSite, csObj, exitCategory);
        });
    }

    /*
     * [HostTarget], <c', o> --category--> <c, r>
     * @params csVar: <c, r>
     * @params csObj: <c', o>
     * @params category: category of host variable (MapKey, MapValue, ColValue)
     */
    private void addTargetToHost(CSVar csVar, CSObj csObj, ContExitCategory category) {
        if (hostManager.addHostTarget(csObj, category, csVar)) {
            CSVar hostPointer = hostManager.getHostVar(csObj, category); // <c', ho>
            solver.addPFGEdge(hostPointer, csVar, FlowKind.HOST_TO_RESULT); // result.getType(),
        }
    }

    /*
     * [HostSource], <c, r> <--category-- <c', o>
     * @params csVar: <c, r>
     * @params csObj: <c', o>
     * @params category: category of host variable (MapKey, MapValue, ColValue)
     */
    private void addSourceToHost(CSVar csArg, CSObj csObj, ContExitCategory category) {
        if (isConcerned(csArg.getVar()) && hostManager.addHostSource(csObj, category, csArg)) {
            CSVar hostPointer = hostManager.getHostVar(csObj, category); // <c', v>
            solver.addPFGEdge(csArg, hostPointer, FlowKind.ARG_TO_HOST);
        }
    }

    // get ptsH for a pointer, usually pointer is a container variable csVar: <c, v>.
    // Sometimes it could be a InstanceField like <c, o>.list
    public PointsToSet getHostListOf(Pointer pointer, HostKind hostKind) {
        return pointerHostListMap.computeIfAbsent(pointer, hostKind, (v, t) -> solver.makePointsToSet());
    }

    // propagate ptsH along PFG edge
    @Override
    public void onNewPFGEdge(PointerFlowEdge edge) {
        Pointer source = edge.source(), target = edge.target();
        if (solver.needPropagateHost(edge)) {
            pointerHostListMap.getOrDefault(source, Collections.emptyMap()).forEach((
                    (hostKind, sourcePtsH) -> solver.addHostEntry(target, hostKind, sourcePtsH)));
        }
    }

    // process l: r = v.k(...) => m
    public static boolean CutReturnEdge(Var lhs, JMethod callee) {
        // if m is a container-exit method, cut m_ret --> r
        if (ContainerConfig.config.getContainerExitCategory(callee) != null)
            return true;
        // if m is a iterator.next method, cut m_ret --> r,
        // here MapEntryVar is to make sure iterator is not Map.entrySet().iterator(), because that next() is Transfer not Exit.
        if (ContainerConfig.config.getIterExitCategory(callee) != null && MapEntryVar.contains(lhs))
            return true;
        // if m is a modeled map.entry.getKey()/getValue(), cut m_ret --> r
        if (ContainerConfig.config.isHostClass(getOuterClass(callee.getDeclaringClass()))
            && isMapEntryClass(callee.getDeclaringClass())
            && (callee.getName().equals("getKey")) || callee.getName().equals("getValue"))
            return true;
        return false;
    }


    // For [Entrance-Extend] generated PFG edges: src --> dst, both src and dst are host variables for container objects.
    // If src may point to a object whose type is not modeled, those host var should be conservatively handled.
    private void taintHost(CSObj hostObj) {
        if (!taintHosts.contains(hostObj)) {
            taintHosts.add(hostObj);
            hostToExits.get(hostObj).forEach(this::recoverCallSite);
            for (ContExitCategory cat: ContExitCategory.values()) {
                hostManager.getHostVar(hostObj, cat).getOutEdges().forEach(outEdge -> {
                    Pointer succ = outEdge.target();
                    if (succ instanceof CSVar csVar) {
                        Obj targetHostObj = hostManager.getHostObj(csVar.getVar());
                        if (targetHostObj != null)
                            taintHost(csManager.getCSObj(csVar.getContext(), targetHostObj));
                    }
                });
            }
        }
    }

    // callsite l: r = v.get(), whose callee may be cut. However, since v may point-to a object whose type is not modeled
    // the PFG from callee to r should be recovered to conservatively model
    private void recoverCallSite(CSCallSite csCallSite) {
        if (solver.addRecoveredCallSite(csCallSite)) {
            CSVar csLHS = csManager.getCSVar(csCallSite.getContext(), csCallSite.getCallSite().getLValue());
            callGraph.getCalleesOf(csCallSite).forEach(csCallee -> {
                JMethod callee = csCallee.getMethod();
                callee.getIR().getReturnVars().forEach(ret -> {
                    CSVar csRet = csManager.getCSVar(csCallee.getContext(), ret); // <c^t, m_ret>
                    solver.addPFGEdge(csRet, csLHS, FlowKind.RETURN); // <c^t, m_ret> --> <c, r>
                });
            });
        }
    }

    // call edge e: <c, l: r = v.k(..)> => <c^t, m> is a exit-method call-relation.
    private void checkHostRelatedExit(CSCallSite csCallSite, CSObj hostObj, ContExitCategory category) {
        Var lhs = csCallSite.getCallSite().getLValue(); // r
        // this callsite can be soundly modeled by cut-shortcut
        if (!solver.isRecoveredCallSite(csCallSite)) {
            if (config.isUnmodeledClass(hostObj.getObject().getType()) || taintHosts.contains(hostObj))
                recoverCallSite(csCallSite);
            else {
                hostToExits.put(hostObj, csCallSite);
                addTargetToHost(csManager.getCSVar(csCallSite.getContext(), lhs), hostObj, category);
            }
        }
    }

}
