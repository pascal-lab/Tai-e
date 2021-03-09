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

package pascal.taie.icfg;

import pascal.taie.callgraph.CallGraph;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.util.CollectionUtils.addToMapSet;
import static pascal.taie.util.CollectionUtils.newMap;

public class JimpleICFG extends AbstractICFG<SootMethod, Unit> {

    private final Map<Unit, Set<Edge<Unit>>> inEdges;
    private final Map<Unit, Set<Edge<Unit>>> outEdges;
    private final Map<Unit, SootMethod> unitToMethod;
    private final Map<SootMethod, DirectedGraph<Unit>> methodToCFG;

    public JimpleICFG(CallGraph<Unit, SootMethod> callGraph) {
        super(callGraph);
        inEdges = newMap();
        outEdges = newMap();
        unitToMethod = newMap();
        methodToCFG = newMap();
        build(callGraph);
    }

    // Implementation of ICFG methods

    @Override
    public Collection<Edge<Unit>> getInEdgesOf(Unit unit) {
        return inEdges.getOrDefault(unit, Collections.emptySet());
    }

    @Override
    public Collection<Edge<Unit>> getOutEdgesOf(Unit unit) {
        return outEdges.getOrDefault(unit, Collections.emptySet());
    }

    @Override
    public Collection<Unit> getEntriesOf(SootMethod method) {
        // TODO - consider multi-head due to unreachable code?
        DirectedGraph<Unit> cfg = getCFGOf(method);
        return cfg.getHeads();
    }

    @Override
    public Collection<Unit> getExitsOf(SootMethod method) {
        // TODO - do exceptional exits matter?
        DirectedGraph<Unit> cfg = getCFGOf(method);
        return cfg.getTails();
    }

    @Override
    public Collection<Unit> getReturnSitesOf(Unit callSite) {
        return getCFGOf(unitToMethod.get(callSite))
                .getSuccsOf(callSite);
    }

    @Override
    public SootMethod getContainingMethodOf(Unit unit) {
        return unitToMethod.get(unit);
    }

    @Override
    public boolean isCallSite(Unit unit) {
        return ((Stmt) unit).containsInvokeExpr();
    }

    // Implementation of DirectGraph methods
    @Override
    public List<Unit> getHeads() {
        return getEntryMethods()
                .stream()
                .map(this::getEntriesOf)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<Unit> getTails() {
        return getEntryMethods()
                .stream()
                .map(this::getExitsOf)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<Unit> getPredsOf(Unit s) {
        return inEdges.get(s)
                .stream()
                .map(Edge::getSource)
                .collect(Collectors.toList());
    }

    @Override
    public List<Unit> getSuccsOf(Unit s) {
        return outEdges.get(s)
                .stream()
                .map(Edge::getTarget)
                .collect(Collectors.toList());
    }

    @Override
    public int size() {
        return unitToMethod.size();
    }

    @Nonnull
    @Override
    public Iterator<Unit> iterator() {
        return unitToMethod.keySet().iterator();
    }

    private void build(CallGraph<Unit, SootMethod> callGraph) {
        for (SootMethod method : callGraph.getReachableMethods()) {
            DirectedGraph<Unit> cfg = getCFGOf(method);
            // TODO - handle special cases
            for (Unit unit : cfg) {
                unitToMethod.put(unit, method);
                // Add local edges
                for (Unit succ : cfg.getSuccsOf(unit)) {
                    Edge<Unit> local = new LocalEdge<>(unit, succ);
                    addToMapSet(outEdges, unit, local);
                    addToMapSet(inEdges, succ, local);
                }
                if (isCallSite(unit)) {
                    for (SootMethod callee : getCalleesOf(unit)) {
                        // Add call edges
                        getEntriesOf(callee).forEach(entry -> {
                            Edge<Unit> call = new CallEdge<>(unit, entry);
                            addToMapSet(outEdges, unit, call);
                            addToMapSet(inEdges, entry, call);
                        });
                        // Add return edges
                        for (Unit exit : getExitsOf(callee)) {
                            for (Unit returnSite : getReturnSitesOf(unit)) {
                                Edge<Unit> ret = new ReturnEdge<>(exit, returnSite, unit);
                                addToMapSet(outEdges, exit, ret);
                                addToMapSet(inEdges, returnSite, ret);
                            }
                        }
                    }
                }
            }
        }
    }

    private DirectedGraph<Unit> getCFGOf(SootMethod method) {
        DirectedGraph<Unit> cfg = methodToCFG.get(method);
        if (cfg == null) {
            // TODO - handle special cases such as native methods
            cfg = new BriefUnitGraph(method.retrieveActiveBody());
            methodToCFG.put(method, cfg);
        }
        return cfg;
    }
}
