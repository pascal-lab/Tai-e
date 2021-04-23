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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.Options;
import pascal.taie.World;
import pascal.taie.analysis.pta.core.cs.element.MapBasedCSManager;
import pascal.taie.analysis.pta.core.cs.selector.ContextInsensitiveSelector;
import pascal.taie.analysis.pta.core.cs.selector.KCallSelector;
import pascal.taie.analysis.pta.core.cs.selector.KObjSelector;
import pascal.taie.analysis.pta.core.cs.selector.KTypeSelector;
import pascal.taie.analysis.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.pta.plugin.AnalysisTimer;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.ReferenceHandler;
import pascal.taie.analysis.pta.plugin.ResultPrinter;
import pascal.taie.analysis.pta.plugin.ThreadHandler;
import pascal.taie.analysis.pta.pts.HybridPointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.util.AnalysisException;

public class PointerAnalysisBuilder {

    public Solver build(Options options) {
        PointsToSetFactory.setFactory(new HybridPointsToSet.Factory());
        SolverImpl solver = new SolverImpl();
        setContextSensitivity(solver, options);
        setPlugin(solver);
        solver.setHeapModel(new AllocationSiteBasedModel(
                World.getTypeManager()));
        solver.setCSManager(new MapBasedCSManager());
        return solver;
    }

    private void setContextSensitivity(SolverImpl solver, Options options) {
        switch (options.getContextSensitivity()) {
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
                throw new AnalysisException(
                        "Unknown context sensitivity variant: "
                                + options.getContextSensitivity());
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
