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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.MapUtils;
import pascal.taie.util.collection.SetUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.analysis.graph.icfg.ICFGBuilder.getCFGOf;

class DefaultICFG extends AbstractICFG<JMethod, Stmt> {

    private static final Logger logger = LogManager.getLogger(DefaultICFG.class);

    private final Map<Stmt, Set<ICFGEdge<Stmt>>> inEdges = MapUtils.newMap();

    private final Map<Stmt, Set<ICFGEdge<Stmt>>> outEdges = MapUtils.newMap();

    private final Map<Stmt, CFG<Stmt>> stmtToCFG = MapUtils.newMap();

    DefaultICFG(CallGraph<Stmt, JMethod> callGraph) {
        super(callGraph);
        build(callGraph);
    }

    private void build(CallGraph<Stmt, JMethod> callGraph) {
        callGraph.reachableMethods().forEach(method -> {
            CFG<Stmt> cfg = getCFGOf(method);
            if (cfg == null) {
                // Since the scope of CFGBuilder only covers application
                // classes for now, while CallGraphBuilder also discovers
                // library classes, the CFG of these library classes
                // are currently absent.
                // FIXME: let CFGBuilder covers the classes analyzed by other analyses.
                logger.warn("CFG of {} is absent", method);
                return;
            }
            cfg.nodes().forEach(stmt -> {
                stmtToCFG.put(stmt, cfg);
                cfg.outEdgesOf(stmt).forEach(edge -> {
                    LocalEdge<Stmt> local = new LocalEdge<>(edge);
                    MapUtils.addToMapSet(outEdges, stmt, local);
                    MapUtils.addToMapSet(inEdges, edge.getTarget(), local);
                });
                if (isCallSite(stmt)) {
                    calleesOf(stmt).forEach(callee -> {
                        if (getCFGOf(callee) == null) { // FIXME: same issues as above
                            return;
                        }
                        // Add call edges
                        Stmt entry = getEntryOf(callee);
                        CallEdge<Stmt> call = new CallEdge<>(stmt, entry);
                        MapUtils.addToMapSet(outEdges, stmt, call);
                        MapUtils.addToMapSet(inEdges, entry, call);
                        // Add return edges
                        Stmt exit = getExitOf(callee);
                        Set<Var> retVars = SetUtils.newHybridSet();
                        Set<ClassType> exceptions = SetUtils.newHybridSet();
                        // The exit node of CFG is mock, thus it is not
                        // a real return or excepting Stmt. We need to
                        // collect return and exception information from
                        // the real return and excepting Stmts, and attach
                        // them to the ReturnEdge.
                        getCFGOf(callee).inEdgesOf(exit).forEach(retEdge -> {
                            if (retEdge.getKind() == Edge.Kind.RETURN) {
                                Return ret = (Return) retEdge.getSource();
                                if (ret.getValue() != null) {
                                    retVars.add(ret.getValue());
                                }
                            }
                            if (retEdge.isExceptional()) {
                                retEdge.exceptions().forEach(exceptions::add);
                            }
                        });
                        returnSitesOf(stmt).forEach(retSite -> {
                            ReturnEdge<Stmt> ret = new ReturnEdge<>(
                                    exit, retSite, stmt, retVars, exceptions);
                            MapUtils.addToMapSet(outEdges, exit, ret);
                            MapUtils.addToMapSet(inEdges, retSite, ret);
                        });
                    });
                }
            });
        });
    }

    @Override
    public Stream<ICFGEdge<Stmt>> inEdgesOf(Stmt stmt) {
        return inEdges.getOrDefault(stmt, Set.of()).stream();
    }

    @Override
    public Stream<ICFGEdge<Stmt>> outEdgesOf(Stmt stmt) {
        return outEdges.getOrDefault(stmt, Set.of()).stream();
    }

    @Override
    public Stmt getEntryOf(JMethod method) {
        return getCFGOf(method).getEntry();
    }

    @Override
    public Stmt getExitOf(JMethod method) {
        return getCFGOf(method).getExit();
    }

    @Override
    public Stream<Stmt> returnSitesOf(Stmt callSite) {
        assert isCallSite(callSite);
        return stmtToCFG.get(callSite).succsOf(callSite);
    }

    @Override
    public JMethod getContainingMethodOf(Stmt stmt) {
        return stmtToCFG.get(stmt).getMethod();
    }

    @Override
    public boolean isCallSite(Stmt stmt) {
        return stmt instanceof Invoke;
    }

    @Override
    public boolean hasNode(Stmt stmt) {
        return stmtToCFG.containsKey(stmt);
    }

    @Override
    public boolean hasEdge(Stmt source, Stmt target) {
        return outEdgesOf(source)
                .anyMatch(edge -> edge.getTarget().equals(target));
    }

    @Override
    public Stream<Stmt> predsOf(Stmt stmt) {
        return inEdgesOf(stmt).map(ICFGEdge::getSource);
    }

    @Override
    public Stream<Stmt> succsOf(Stmt stmt) {
        return outEdgesOf(stmt).map(ICFGEdge::getTarget);
    }

    @Override
    public Stream<Stmt> nodes() {
        return stmtToCFG.keySet().stream();
    }

    @Override
    public int getNumberOfNodes() {
        return stmtToCFG.size();
    }
}
