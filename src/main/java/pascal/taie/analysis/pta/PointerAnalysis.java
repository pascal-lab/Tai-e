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

import pascal.taie.World;
import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.analysis.pta.core.cs.element.MapBasedCSManager;
import pascal.taie.analysis.pta.core.cs.selector.ContextInsensitiveSelector;
import pascal.taie.analysis.pta.core.cs.selector.KCallSelector;
import pascal.taie.analysis.pta.core.cs.selector.KObjSelector;
import pascal.taie.analysis.pta.core.cs.selector.KTypeSelector;
import pascal.taie.analysis.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.pta.core.solver.SolverImpl;
import pascal.taie.analysis.pta.plugin.AnalysisTimer;
import pascal.taie.analysis.pta.plugin.ClassInitializer;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.ReferenceHandler;
import pascal.taie.analysis.pta.plugin.ResultProcessor;
import pascal.taie.analysis.pta.plugin.ThreadHandler;
import pascal.taie.analysis.pta.plugin.invokedynamic.InvokeDynamicPlugin;
import pascal.taie.analysis.pta.plugin.invokedynamic.LambdaPlugin;
import pascal.taie.analysis.pta.plugin.reflection.ReflectionPlugin;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.ConfigException;

public class PointerAnalysis extends InterproceduralAnalysis {

    public static final String ID = "pta";

    public PointerAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public PointerAnalysisResult analyze() {
        SolverImpl solver = new SolverImpl();
        setContextSensitivity(solver);
        solver.setOptions(getOptions());
        solver.setHeapModel(new AllocationSiteBasedModel(getOptions()));
        solver.setCSManager(new MapBasedCSManager());
        // The initialization of some Plugins may read the fields in solver,
        // e.g., contextSelector or csManager, thus we initialize Plugins
        // after setting all other fields of solver.
        setPlugin(solver);
        solver.solve();
        return solver.getResult();
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
        // To record elapsed time precisely, AnalysisTimer should be added at first.
        // TODO: remove such order dependency
        plugin.addPlugin(
                new AnalysisTimer(),
                new ClassInitializer(),
                new ThreadHandler(),
                new ReflectionPlugin(),
                new ResultProcessor()
        );
        if (World.getOptions().getJavaVersion() < 9) {
            // current reference model doesn't support Java 9+
            plugin.addPlugin(new ReferenceHandler());
        }
        if (World.getOptions().getJavaVersion() >= 7) {
            plugin.addPlugin(new InvokeDynamicPlugin());
        }
        if (World.getOptions().getJavaVersion() >= 8) {
            plugin.addPlugin(new LambdaPlugin());
        }
        plugin.setSolver(solver);
        solver.setPlugin(plugin);
    }
}
