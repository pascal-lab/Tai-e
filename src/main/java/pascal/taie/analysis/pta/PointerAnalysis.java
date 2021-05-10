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

package pascal.taie.analysis.pta;

import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.analysis.pta.core.cs.element.MapBasedCSManager;
import pascal.taie.analysis.pta.core.cs.selector.ContextInsensitiveSelector;
import pascal.taie.analysis.pta.core.cs.selector.KCallSelector;
import pascal.taie.analysis.pta.core.cs.selector.KObjSelector;
import pascal.taie.analysis.pta.core.cs.selector.KTypeSelector;
import pascal.taie.analysis.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.SolverImpl;
import pascal.taie.analysis.pta.plugin.AnalysisTimer;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.ReferenceHandler;
import pascal.taie.analysis.pta.plugin.ResultPrinter;
import pascal.taie.analysis.pta.plugin.ThreadHandler;
import pascal.taie.analysis.pta.pts.HybridPointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.ConfigException;

public class PointerAnalysis extends InterproceduralAnalysis {

    public static final String ID = "pta";

    public PointerAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Solver analyze() {
        PointsToSetFactory.setFactory(new HybridPointsToSet.Factory());
        SolverImpl solver = new SolverImpl();
        setContextSensitivity(solver);
        setPlugin(solver);
        solver.setOptions(getOptions());
        solver.setHeapModel(new AllocationSiteBasedModel(getOptions()));
        solver.setCSManager(new MapBasedCSManager());
        solver.solve();
        // TODO: add a class to represent pointer analysis results, including
        //  points-to set, call graph, exception, etc., without contexts
        return solver;
    }

    private void setContextSensitivity(SolverImpl solver) {
        switch (getOptions().getString("cs")) {
            case "ci":
                solver.setContextSelector(new ContextInsensitiveSelector());
                break;
            case "1-call":
            case "1-cfa":
                solver.setContextSelector(new KCallSelector(1));
                break;
            case "1-obj":
            case "1-object":
                solver.setContextSelector(new KObjSelector(1));
                break;
            case "1-type":
                solver.setContextSelector(new KTypeSelector(1));
                break;
            case "2-call":
            case "2-cfa":
                solver.setContextSelector(new KCallSelector(2));
                break;
            case "2-obj":
            case "2-object":
                solver.setContextSelector(new KObjSelector(2));
                break;
            case "2-type":
                solver.setContextSelector(new KTypeSelector(2));
                break;
            default:
                throw new ConfigException(
                        "Unknown context sensitivity variant: "
                                + getOptions().getString("cs"));
        }
    }

    private void setPlugin(SolverImpl solver) {
        CompositePlugin plugin = new CompositePlugin();
        // To record elapsed time precisely, AnalysisTimer should be
        // added at first.
        // TODO: remove such order dependency
        plugin.addPlugin(
                new AnalysisTimer(),
                new ThreadHandler(),
                new ReferenceHandler(),
                ResultPrinter.get()
        );
        plugin.setSolver(solver);
        solver.setPlugin(plugin);
    }
}
