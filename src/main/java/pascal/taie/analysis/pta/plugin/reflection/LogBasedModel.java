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

package pascal.taie.analysis.pta.plugin.reflection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class LogBasedModel extends MetaObjModel {

    private static final Logger logger = LogManager.getLogger(LogBasedModel.class);

    private final Set<String> supportedApis = Set.of(
            "Class.forName",
            "Class.newInstance",
            "Constructor.newInstance",
            "Method.invoke",
            "Field.get",
            "Field.set",
            "Array.newInstance"
    );

    private final Map<String, String> fullNames = Map.of(
            "Class", ClassNames.CLASS,
            "Constructor", ClassNames.CONSTRUCTOR,
            "Method", ClassNames.METHOD,
            "Field", ClassNames.FIELD,
            "Array", ClassNames.ARRAY
    );

    private final Set<JMethod> relevantMethods = Sets.newSet();

    private final MultiMap<Invoke, JClass> forNameTargets = Maps.newMultiMap();

    private final MultiMap<Invoke, ClassType> arrayTypeTargets = Maps.newMultiMap();

    private final MultiMap<Invoke, JClass> classTargets = Maps.newMultiMap();

    private final MultiMap<Invoke, ClassMember> memberTargets = Maps.newMultiMap();

    private final ContextSelector selector;

    LogBasedModel(Solver solver) {
        super(solver);
        selector = solver.getContextSelector();
        String path = solver.getOptions().getString("reflection-log");
        logger.info("Using reflection log from {}", path);
        LogItem.load(path).forEach(this::addItem);
    }

    private void addItem(LogItem item) {
        if (!supportedApis.contains(item.api)) {
            return;
        }
        // obtain reflective target
        Object target = switch (item.api) {
            case "Class.forName", "Class.newInstance" ->
                    hierarchy.getClass(item.target);
            case "Constructor.newInstance", "Method.invoke" ->
                    hierarchy.getMethod(item.target);
            case "Field.get", "Field.set" ->
                    hierarchy.getField(item.target);
            case "Array.newInstance" -> {
                // Note that currently we only support Array.newInstance(Class,int),
                // and ignore primitive arrays.
                String baseName = StringReps.getBaseTypeNameOf(item.target);
                JClass baseClass = hierarchy.getClass(baseName);
                yield baseClass != null ? baseClass.getType() : null;
            }
            default -> null;
        };
        // add target specified in the item
        if (target != null) {
            List<Invoke> invokes = getMatchedInvokes(item);
            if (target instanceof JClass) {
                if (item.api.equals("Class.forName")) {
                    for (Invoke invoke : invokes) {
                        forNameTargets.put(invoke, (JClass) target);
                    }
                } else {
                    for (Invoke invoke : invokes) {
                        classTargets.put(invoke, (JClass) target);
                    }
                }
            } else if (target instanceof ClassMember) {
                for (Invoke invoke : invokes) {
                    memberTargets.put(invoke, (ClassMember) target);
                }
            } else {
                for (Invoke invoke : invokes) {
                    arrayTypeTargets.put(invoke, (ClassType) target);
                }
            }
            invokes.stream()
                    .map(Invoke::getContainer)
                    .forEach(relevantMethods::add);
        } else {
            logger.warn("Target '{}' is not found", item.target);
        }
    }

    private List<Invoke> getMatchedInvokes(LogItem item) {
        int lastDot = item.caller.lastIndexOf('.');
        String callerClass = item.caller.substring(0, lastDot);
        String callerMethod = item.caller.substring(lastDot + 1);
        JClass klass = hierarchy.getClass(callerClass);
        if (klass == null) {
            logger.warn("Class '{}' is absent", callerClass);
            return List.of();
        }
        List<Invoke> invokes = new ArrayList<>();
        klass.getDeclaredMethods()
                .stream()
                .filter(m -> m.getName().equals(callerMethod) && !m.isAbstract())
                .forEach(caller ->
                        caller.getIR()
                                .stmts()
                                .filter(s -> s instanceof Invoke)
                                .forEach(s -> {
                                    Invoke invoke = (Invoke) s;
                                    if (isMatched(item, invoke)) {
                                        invokes.add(invoke);
                                    }
                                })
                );
        if (invokes.isEmpty()) {
            logger.warn("No matched invokes found for {}/{}",
                    item.caller, item.lineNumber);
        }
        return invokes;
    }

    private boolean isMatched(LogItem item, Invoke invoke) {
        if (invoke.isDynamic()) {
            return false;
        }
        int lastDot = item.api.lastIndexOf('.');
        String apiClass = fullNames.get(item.api.substring(0, lastDot));
        String apiMethod = item.api.substring(lastDot + 1);
        JMethod callee = invoke.getMethodRef().resolve();
        return callee.getDeclaringClass().getName().equals(apiClass) &&
                callee.getName().equals(apiMethod) &&
                (item.lineNumber == LogItem.UNKNOWN ||
                        item.lineNumber == invoke.getLineNumber());
    }

    @Override
    void handleNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        if (relevantMethods.contains(method)) {
            method.getIR()
                    .stmts()
                    .filter(s -> s instanceof Invoke)
                    .map(s -> (Invoke) s)
                    .forEach(invoke -> {
                        handleForName(csMethod, invoke);
                        passTargetToBase(classTargets, csMethod, invoke);
                        passTargetToBase(memberTargets, csMethod, invoke);
                        passTargetToArg0(arrayTypeTargets, csMethod, invoke);
                    });
        }
    }

    private void handleForName(CSMethod csMethod, Invoke invoke) {
        if (forNameTargets.containsKey(invoke)) {
            Context context = csMethod.getContext();
            Var result = invoke.getResult();
            forNameTargets.get(invoke).forEach(target -> {
                solver.initializeClass(target);
                if (result != null) {
                    solver.addVarPointsTo(context, result,
                            toCSObj(csMethod, target));
                }
            });
        }
    }

    private <T> void passTargetToBase(MultiMap<Invoke, T> targetMap,
                                      CSMethod csMethod, Invoke invoke) {
        passTarget(targetMap, csMethod, invoke,
                i -> ((InvokeInstanceExp) i.getInvokeExp()).getBase());
    }

    private <T> void passTargetToArg0(MultiMap<Invoke, T> targetMap,
                                      CSMethod csMethod, Invoke invoke) {
        passTarget(targetMap, csMethod, invoke,
                i -> i.getInvokeExp().getArg(0));
    }

    private <T> void passTarget(
            MultiMap<Invoke, T> targetMap, CSMethod csMethod,
            Invoke invoke, Function<Invoke, Var> varGetter) {
        if (targetMap.containsKey(invoke)) {
            Context context = csMethod.getContext();
            Var var = varGetter.apply(invoke);
            targetMap.get(invoke).forEach(target ->
                    solver.addVarPointsTo(context, var, toCSObj(csMethod, target)));
        }
    }

    private CSObj toCSObj(CSMethod csMethod, Object target) {
        Obj obj;
        if (target instanceof JClass) {
            obj = heapModel.getConstantObj(
                    ClassLiteral.get(((JClass) target).getType()));
        } else if (target instanceof ClassMember) {
            obj = getReflectionObj((ClassMember) target);
        } else {
            obj = heapModel.getConstantObj(
                    ClassLiteral.get((ClassType) target));
        }
        Context hctx = selector.selectHeapContext(csMethod, obj);
        return csManager.getCSObj(hctx, obj);
    }

    // Following methods are useless in this class, thus we provide
    // empty implementation.
    @Override
    protected void registerVarAndHandler() {
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
    }

    @Override
    public boolean isRelevantVar(Var var) {
        return false;
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
    }
}
