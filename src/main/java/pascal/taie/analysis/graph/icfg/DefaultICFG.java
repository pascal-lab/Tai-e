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

package pascal.taie.analysis.graph.icfg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGEdge;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static pascal.taie.analysis.graph.icfg.ICFGBuilder.getCFGOf;

class DefaultICFG extends AbstractICFG<JMethod, Stmt> {

    private static final Logger logger = LogManager.getLogger(DefaultICFG.class);

    private final MultiMap<Stmt, ICFGEdge<Stmt>> inEdges = Maps.newMultiMap();

    private final MultiMap<Stmt, ICFGEdge<Stmt>> outEdges = Maps.newMultiMap();

    private final Map<Stmt, CFG<Stmt>> stmtToCFG = Maps.newLinkedHashMap();

    DefaultICFG(CallGraph<Stmt, JMethod> callGraph) {
        super(callGraph);
        build(callGraph);
    }

    private void build(CallGraph<Stmt, JMethod> callGraph) {
        callGraph.forEach(method -> {
            CFG<Stmt> cfg = getCFGOf(method);
            if (cfg == null) {
                logger.warn("CFG of {} is absent, try to fix this" +
                        " by adding option: -scope REACHABLE", method);
                return;
            }
            cfg.forEach(stmt -> {
                stmtToCFG.put(stmt, cfg);
                cfg.getOutEdgesOf(stmt).forEach(edge -> {
                    ICFGEdge<Stmt> local = isCallSite(stmt) ?
                            new CallToReturnEdge<>(edge) :
                            new NormalEdge<>(edge);
                    outEdges.put(stmt, local);
                    inEdges.put(edge.target(), local);
                });
                if (isCallSite(stmt)) {
                    getCalleesOf(stmt).forEach(callee -> {
                        if (getCFGOf(callee) == null) {
                            logger.warn("CFG of {} is missing", callee);
                            return;
                        }
                        // Add call edges
                        Stmt entry = getEntryOf(callee);
                        CallEdge<Stmt> call = new CallEdge<>(stmt, entry, callee);
                        outEdges.put(stmt, call);
                        inEdges.put(entry, call);
                        // Add return edges
                        Stmt exit = getExitOf(callee);
                        Set<Var> retVars = Sets.newHybridSet();
                        Set<ClassType> exceptions = Sets.newHybridSet();
                        // The exit node of CFG is mock, thus it is not
                        // a real return or excepting Stmt. We need to
                        // collect return and exception information from
                        // the real return and excepting Stmts, and attach
                        // them to the ReturnEdge.
                        getCFGOf(callee).getInEdgesOf(exit).forEach(retEdge -> {
                            if (retEdge.getKind() == CFGEdge.Kind.RETURN) {
                                Return ret = (Return) retEdge.source();
                                if (ret.getValue() != null) {
                                    retVars.add(ret.getValue());
                                }
                            }
                            if (retEdge.isExceptional()) {
                                exceptions.addAll(retEdge.getExceptions());
                            }
                        });
                        getReturnSitesOf(stmt).forEach(retSite -> {
                            ReturnEdge<Stmt> ret = new ReturnEdge<>(
                                    exit, retSite, stmt, retVars, exceptions);
                            outEdges.put(exit, ret);
                            inEdges.put(retSite, ret);
                        });
                    });
                }
            });
        });
    }

    @Override
    public Set<ICFGEdge<Stmt>> getInEdgesOf(Stmt stmt) {
        return inEdges.get(stmt);
    }

    @Override
    public Set<ICFGEdge<Stmt>> getOutEdgesOf(Stmt stmt) {
        return outEdges.get(stmt);
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
    public Set<Stmt> getReturnSitesOf(Stmt callSite) {
        assert isCallSite(callSite);
        return stmtToCFG.get(callSite).getSuccsOf(callSite);
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
    public boolean hasEdge(Stmt source, Stmt target) {
        return getOutEdgesOf(source)
                .stream()
                .anyMatch(edge -> edge.target().equals(target));
    }

    @Override
    public Set<Stmt> getPredsOf(Stmt stmt) {
        return Views.toMappedSet(getInEdgesOf(stmt), ICFGEdge::source);
    }

    @Override
    public Set<Stmt> getSuccsOf(Stmt stmt) {
        return Views.toMappedSet(getOutEdgesOf(stmt), ICFGEdge::target);
    }

    @Override
    public Set<Stmt> getNodes() {
        return Collections.unmodifiableSet(stmtToCFG.keySet());
    }
}
