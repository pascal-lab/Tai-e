/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.dataflow.analysis.livevar;

import panda.dataflow.analysis.DataFlowAnalysis;
import panda.dataflow.lattice.DataFlowTag;
import panda.dataflow.lattice.FlowSet;
import panda.dataflow.lattice.HashFlowSet;
import panda.dataflow.solver.Solver;
import panda.dataflow.solver.SolverFactory;
import panda.util.SootUtils;
import soot.Body;
import soot.BodyTransformer;
import soot.BriefUnitPrinter;
import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.Map;

public class LiveVariableAnalysis extends BodyTransformer
        implements DataFlowAnalysis<FlowSet<Local>, Unit> {

    private static final LiveVariableAnalysis INSTANCE = new LiveVariableAnalysis();
    private static boolean isOutput = true;

    private LiveVariableAnalysis() {
    }

    public static LiveVariableAnalysis v() {
        return INSTANCE;
    }

    public static void setOutput(boolean isOutput) {
        LiveVariableAnalysis.isOutput = isOutput;
    }

    // ---------- Data-flow analysis for live variable analysis  ----------
    @Override
    public boolean isForward() {
        return false;
    }

    @Override
    public FlowSet<Local> getEntryInitialFlow(Unit entry) {
        return newInitialFlow();
    }

    @Override
    public FlowSet<Local> newInitialFlow() {
        return new HashFlowSet<>();
    }

    @Override
    public FlowSet<Local> meet(FlowSet<Local> v1, FlowSet<Local> v2) {
        return v1.duplicate().union(v2);
    }

    @Override
    public boolean transfer(Unit unit, FlowSet<Local> in, FlowSet<Local> out) {
        FlowSet<Local> oldOut = out.duplicate();
        out.setTo(in);
        // Kill definitions in unit
        //noinspection SuspiciousMethodCalls
        unit.getDefBoxes()
                .stream()
                .map(ValueBox::getValue)
                .filter(v -> v instanceof Local)
                .forEach(out::remove);
        // Generate uses in unit
        unit.getUseBoxes()
                .stream()
                .map(ValueBox::getValue)
                .filter(v -> v instanceof Local)
                .forEach(v -> out.add((Local) v));
        return !out.equals(oldOut);
    }

    // ---------- Body transformer ----------
    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        DirectedGraph<Unit> cfg = new BriefUnitGraph(b);
        Solver<FlowSet<Local>, Unit> solver = SolverFactory.v().newSolver(this, cfg);
        solver.solve();
        b.addTag(new DataFlowTag<>("LiveVarTag", solver.getAfterFlow()));
        if (isOutput) {
            outputResult(b, solver.getAfterFlow());
        }
    }

    private synchronized void outputResult(Body body, Map<Unit, FlowSet<Local>> result) {
        System.out.println("------ " + body.getMethod() + " [live variables] -----");
        BriefUnitPrinter up = new BriefUnitPrinter(body);
        body.getUnits().forEach(u ->
                System.out.println(SootUtils.unitToString(up, u)
                        + ": " + result.get(u)));
    }
}
