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
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.MapUtils;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class DefaultICFG extends AbstractICFG<JMethod, Stmt> {

    private final Map<Stmt, Set<ICFGEdge<Stmt>>> inEdges;

    private final Map<Stmt, Set<ICFGEdge<Stmt>>> outEdges;

    private final Map<Stmt, JMethod> stmtToMethod;

    /**
     * Obtains the CFG of given method.
     */
    private final Function<JMethod, CFG<Stmt>> cfgGetter;

    DefaultICFG(CallGraph<Stmt, JMethod> callGraph,
                Function<JMethod, CFG<Stmt>> cfgGetter) {
        super(callGraph);
        inEdges = MapUtils.newMap();
        outEdges = MapUtils.newMap();
        stmtToMethod = MapUtils.newMap();
        this.cfgGetter = cfgGetter;
    }
//
//    @Override
//    public Collection<ICFGEdge<Stmt>> inEdesOf(Stmt stmt) {
//        return inEdges.getOrDefault(stmt, Set.of());
//    }
//
//    @Override
//    public Collection<ICFGEdge<Stmt>> getOutEdgesOf(Stmt stmt) {
//        return outEdges.getOrDefault(stmt, Set.of());
//    }
//
//    @Override
//    public Collection<Stmt> getEntriesOf(JMethod method) {
//        CFG<Stmt> cfg = cfgGetter.apply(method);
//        return List.of(cfg.getEntry());
//    }
//
//    @Override
//    public Collection<Stmt> getExitsOf(JMethod method) {
//        // TODO - do exceptional exits matter?
//        CFG<Stmt> cfg = cfgGetter.apply(method);
//        return List.of(cfg.getExit());
//    }
//
//    @Override
//    public Collection<Stmt> getReturnSitesOf(Stmt callSite) {
//        return cfgGetter.apply(stmtToMethod.get(callSite))
//                .succsOf(callSite)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public JMethod getContainingMethodOf(Stmt stmt) {
//        return stmtToMethod.get(stmt);
//    }
//
//    @Override
//    public boolean isCallSite(Stmt stmt) {
//        return stmt instanceof Invoke;
//    }
//
//    // Implementation of DirectGraph methods
//    @Override
//    public List<Stmt> getHeads() {
//        return getEntryMethods()
//                .stream()
//                .map(this::getEntriesOf)
//                .flatMap(Collection::stream)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Stmt> getTails() {
//        return getEntryMethods()
//                .stream()
//                .map(this::getExitsOf)
//                .flatMap(Collection::stream)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Stmt> getPredsOf(Stmt s) {
//        return inEdges.get(s)
//                .stream()
//                .map(ICFGEdge::getSource)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Stmt> getSuccsOf(Stmt s) {
//        return outEdges.get(s)
//                .stream()
//                .map(ICFGEdge::getTarget)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public int size() {
//        return stmtToMethod.size();
//    }
//
//    @Nonnull
//    @Override
//    public Iterator<Stmt> iterator() {
//        return stmtToMethod.keySet().iterator();
//    }
}
