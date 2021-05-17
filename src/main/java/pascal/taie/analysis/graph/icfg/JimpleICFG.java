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

package pascal.taie.analysis.graph.icfg;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.util.collection.MapUtils.addToMapSet;
import static pascal.taie.util.collection.MapUtils.newMap;

public class JimpleICFG extends AbstractICFG<SootMethod, Unit> {

    private final Map<Unit, Set<ICFGEdge<Unit>>> inEdges;
    private final Map<Unit, Set<ICFGEdge<Unit>>> outEdges;
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
    public Stream<ICFGEdge<Unit>> inEdgesOf(Unit unit) {
        return inEdges.getOrDefault(unit, Set.of()).stream();
    }

    @Override
    public Stream<ICFGEdge<Unit>> outEdgesOf(Unit unit) {
        return outEdges.getOrDefault(unit, Set.of()).stream();
    }

    @Override
    public Stream<Unit> entriesOf(SootMethod method) {
        // TODO - consider multi-head due to unreachable code?
        DirectedGraph<Unit> cfg = getCFGOf(method);
        return cfg.getHeads().stream();
    }

    @Override
    public Stream<Unit> exitsOf(SootMethod method) {
        // TODO - do exceptional exits matter?
        DirectedGraph<Unit> cfg = getCFGOf(method);
        return cfg.getTails().stream();
    }

    @Override
    public Stream<Unit> returnSitesOf(Unit callSite) {
        return getCFGOf(unitToMethod.get(callSite))
                .getSuccsOf(callSite)
                .stream();
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
        return entryMethods()
                .flatMap(this::entriesOf)
                .collect(Collectors.toList());
    }

    @Override
    public List<Unit> getTails() {
        return entryMethods()
                .flatMap(this::exitsOf)
                .collect(Collectors.toList());
    }

    @Override
    public List<Unit> getPredsOf(Unit s) {
        return inEdges.get(s)
                .stream()
                .map(ICFGEdge::getSource)
                .collect(Collectors.toList());
    }

    @Override
    public List<Unit> getSuccsOf(Unit s) {
        return outEdges.get(s)
                .stream()
                .map(ICFGEdge::getTarget)
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
        callGraph.reachableMethods().forEach(method -> {
            DirectedGraph<Unit> cfg = getCFGOf(method);
            // TODO - handle special cases
            for (Unit unit : cfg) {
                unitToMethod.put(unit, method);
                // Add local edges
                for (Unit succ : cfg.getSuccsOf(unit)) {
                    ICFGEdge<Unit> local = new LocalEdge<>(unit, succ);
                    addToMapSet(outEdges, unit, local);
                    addToMapSet(inEdges, succ, local);
                }
                if (isCallSite(unit)) {
                    calleesOf(unit).forEach(callee -> {
                        // Add call edges
                        entriesOf(callee).forEach(entry -> {
                            ICFGEdge<Unit> call = new CallEdge<>(unit, entry);
                            addToMapSet(outEdges, unit, call);
                            addToMapSet(inEdges, entry, call);
                        });
                        // Add return edges
                        exitsOf(callee).forEach(exit -> {
                            returnSitesOf(unit).forEach(returnSite -> {
                                ICFGEdge<Unit> ret = new ReturnEdge<>(exit, returnSite, unit);
                                addToMapSet(outEdges, exit, ret);
                                addToMapSet(inEdges, returnSite, ret);
                            });
                        });
                    });
                }
            }
        });
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
