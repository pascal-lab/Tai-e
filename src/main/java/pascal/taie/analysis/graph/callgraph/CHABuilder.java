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
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.ArrayDeque;
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
    private TwoKeyMap<JClass, MemberRef, Set<JMethod>> resolveTable;

    @Override
    public CallGraph<Invoke, JMethod> build() {
        return buildCallGraph(World.get().getMainMethod());
    }

    private CallGraph<Invoke, JMethod> buildCallGraph(JMethod entry) {
        hierarchy = World.get().getClassHierarchy();
        resolveTable = Maps.newTwoKeyMap();
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryMethod(entry);
        Queue<JMethod> workList = new ArrayDeque<>();
        workList.add(entry);
        while (!workList.isEmpty()) {
            JMethod method = workList.poll();
            if (callGraph.addReachableMethod(method)) {
                callGraph.callSitesIn(method).forEach(invoke -> {
                    Set<JMethod> callees = resolveCalleesOf(invoke);
                    callees.forEach(callee -> {
                        if (!callGraph.contains(callee)) {
                            workList.add(callee);
                        }
                        callGraph.addEdge(new Edge<>(
                                CallGraphs.getCallKind(invoke), invoke, callee));
                    });
                });
            }
        }
        return callGraph;
    }

    /**
     * Resolves callees of a call site via class hierarchy analysis.
     */
    private Set<JMethod> resolveCalleesOf(Invoke callSite) {
        CallKind kind = CallGraphs.getCallKind(callSite);
        return switch (kind) {
            case INTERFACE, VIRTUAL -> {
                MethodRef methodRef = callSite.getMethodRef();
                JClass cls = methodRef.getDeclaringClass();
                Set<JMethod> callees = resolveTable.get(cls, methodRef);
                if (callees == null) {
                    callees = hierarchy.getAllSubclassesOf(cls)
                            .stream()
                            .filter(Predicate.not(JClass::isAbstract))
                            .map(c -> hierarchy.dispatch(c, methodRef))
                            .filter(Objects::nonNull) // filter out null callees
                            .collect(Collectors.toUnmodifiableSet());
                    resolveTable.put(cls, methodRef, callees);
                }
                yield callees;
            }
            case SPECIAL, STATIC -> Set.of(callSite.getMethodRef().resolve());
            case DYNAMIC -> {
                logger.debug("CHA cannot resolve invokedynamic " + callSite);
                yield Set.of();
            }
            default -> throw new AnalysisException(
                    "Failed to resolve call site: " + callSite);
        };
    }
}
