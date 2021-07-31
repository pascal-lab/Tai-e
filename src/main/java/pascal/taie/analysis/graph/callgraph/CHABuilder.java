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

package pascal.taie.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.MapUtils;

import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds call graph via class hierarchy analysis.
 */
class CHABuilder implements CGBuilder<Invoke, JMethod> {

    private static final Logger logger = LogManager.getLogger(CHABuilder.class);

    private ClassHierarchy hierarchy;

    /**
     * Cache resolve results for interface/virtual invocations.
     */
    private Map<JClass, Map<MemberRef, Set<JMethod>>> resolveTable;

    @Override
    public CallGraph<Invoke, JMethod> build() {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryMethod(World.getMainMethod());
        buildCallGraph(callGraph);
        return callGraph;
    }

    private void buildCallGraph(DefaultCallGraph callGraph) {
        hierarchy = World.getClassHierarchy();
        resolveTable = MapUtils.newMap();
        Queue<JMethod> queue = new LinkedList<>();
        CollectionUtils.addAll(queue, callGraph.entryMethods());
        while (!queue.isEmpty()) {
            JMethod method = queue.remove();
            callGraph.callSitesIn(method).forEach(invoke -> {
                Set<JMethod> callees = resolveCalleesOf(invoke);
                callees.forEach(callee -> {
                    if (!callGraph.contains(callee)) {
                        queue.add(callee);
                    }
                    callGraph.addEdge(new Edge<>(
                            CGUtils.getCallKind(invoke), invoke, callee));
                });
            });
        }
        hierarchy = null;
        resolveTable = null;
    }

    /**
     * Resolves callees of a call site via class hierarchy analysis.
     */
    private Set<JMethod> resolveCalleesOf(Invoke callSite) {
        CallKind kind = CGUtils.getCallKind(callSite);
        switch (kind) {
            case INTERFACE:
            case VIRTUAL: {
                MethodRef methodRef = callSite.getMethodRef();
                JClass cls = methodRef.getDeclaringClass();
                Set<JMethod> callees = MapUtils.getMapMap(resolveTable, cls, methodRef);
                if (callees != null) {
                    return callees;
                }
                callees = hierarchy.getAllSubclassesOf(cls, true)
                        .stream()
                        .filter(Predicate.not(JClass::isAbstract))
                        .map(c -> hierarchy.dispatch(c, methodRef))
                        .filter(Objects::nonNull) // filter out null callees
                        .collect(Collectors.toUnmodifiableSet());
                MapUtils.addToMapMap(resolveTable, cls, methodRef, callees);
                return callees;
            }
            case SPECIAL:
            case STATIC: {
                return Set.of(callSite.getMethodRef().resolve());
            }
            case DYNAMIC: {
                logger.debug("CHA cannot resolve invokedynamic " + callSite);
                return Set.of();
            }
            default:
                throw new AnalysisException("Failed to resolve call site: " + callSite);
        }
    }
}
