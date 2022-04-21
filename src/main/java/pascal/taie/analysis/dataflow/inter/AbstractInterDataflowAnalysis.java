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

package pascal.taie.analysis.dataflow.inter;

import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.graph.icfg.CallEdge;
import pascal.taie.analysis.graph.icfg.CallToReturnEdge;
import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.analysis.graph.icfg.ICFGBuilder;
import pascal.taie.analysis.graph.icfg.ICFGEdge;
import pascal.taie.analysis.graph.icfg.NormalEdge;
import pascal.taie.analysis.graph.icfg.ReturnEdge;
import pascal.taie.config.AnalysisConfig;

/**
 * Provides common functionalities for {@link InterDataflowAnalysis} implementations.
 *
 * @param <Method> type of ICFG edges
 * @param <Node>   type of ICFG nodes
 * @param <Fact>   type of data-flow facts
 */
public abstract class AbstractInterDataflowAnalysis<Method, Node, Fact>
        extends ProgramAnalysis<DataflowResult<Node, Fact>>
        implements InterDataflowAnalysis<Node, Fact> {

    protected ICFG<Method, Node> icfg;

    protected InterSolver<Method, Node, Fact> solver;

    public AbstractInterDataflowAnalysis(AnalysisConfig config) {
        super(config);
    }

    /**
     * If the concrete analysis needs to perform some initialization before
     * the solver starts, then it can overwrite this method.
     */
    protected void initialize() {
    }

    /**
     * If the concrete analysis needs to perform some finishing work after
     * the solver finishes, then it can overwrite this method.
     */
    protected void finish() {
    }

    /**
     * Dispatches {@code Node} to specific node transfer functions for
     * call nodes and non-call nodes.
     */
    @Override
    public boolean transferNode(Node node, Fact in, Fact out) {
        if (icfg.isCallSite(node)) {
            return transferCallNode(node, in, out);
        } else {
            return transferNonCallNode(node, in, out);
        }
    }

    /**
     * Transfer function for call node.
     */
    protected abstract boolean transferCallNode(Node node, Fact in, Fact out);

    /**
     * Transfer function for non-call node.
     */
    protected abstract boolean transferNonCallNode(Node node, Fact in, Fact out);

    /**
     * Dispatches {@link ICFGEdge} to specific edge transfer functions
     * according to the concrete type of {@link ICFGEdge}.
     */
    @Override
    public Fact transferEdge(ICFGEdge<Node> edge, Fact out) {
        if (edge instanceof NormalEdge) {
            return transferNormalEdge((NormalEdge<Node>) edge, out);
        } else if (edge instanceof CallToReturnEdge) {
            return transferCallToReturnEdge((CallToReturnEdge<Node>) edge, out);
        } else if (edge instanceof CallEdge) {
            return transferCallEdge((CallEdge<Node>) edge, out);
        } else {
            return transferReturnEdge((ReturnEdge<Node>) edge, out);
        }
    }

    // ---------- transfer functions for specific ICFG edges ----------
    protected abstract Fact transferNormalEdge(NormalEdge<Node> edge, Fact out);

    protected abstract Fact transferCallToReturnEdge(CallToReturnEdge<Node> edge, Fact out);

    protected abstract Fact transferCallEdge(CallEdge<Node> edge, Fact callSiteOut);

    protected abstract Fact transferReturnEdge(ReturnEdge<Node> edge, Fact returnOut);
    // ----------------------------------------------------------------

    @Override
    public DataflowResult<Node, Fact> analyze() {
        icfg = World.get().getResult(ICFGBuilder.ID);
        initialize();
        solver = new InterSolver<>(this, icfg);
        DataflowResult<Node, Fact> result = solver.solve();
        finish();
        return result;
    }
}
