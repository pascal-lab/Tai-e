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

import org.apache.logging.log4j.Level;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.pta.core.cs.element.MapBasedCSManager;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelectorFactory;
import pascal.taie.analysis.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.solver.DefaultSolver;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.AnalysisTimer;
import pascal.taie.analysis.pta.plugin.ClassInitializer;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.ReferenceHandler;
import pascal.taie.analysis.pta.plugin.ResultProcessor;
import pascal.taie.analysis.pta.plugin.ThreadHandler;
import pascal.taie.analysis.pta.plugin.exception.ExceptionAnalysis;
import pascal.taie.analysis.pta.plugin.invokedynamic.InvokeDynamicAnalysis;
import pascal.taie.analysis.pta.plugin.invokedynamic.LambdaAnalysis;
import pascal.taie.analysis.pta.plugin.reflection.ReflectionAnalysis;
import pascal.taie.analysis.pta.plugin.taint.TaintAnalysis;
import pascal.taie.analysis.pta.toolkit.scaler.Scaler;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.util.Timer;

public class PointerAnalysis extends ProgramAnalysis {

    public static final String ID = "pta";

    public PointerAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public PointerAnalysisResult analyze() {
        AnalysisOptions options = getOptions();
        HeapModel heapModel = new AllocationSiteBasedModel(options);
        ContextSelector selector;
        String cs = options.getString("cs");
        if (cs.equals("scaler")) {
            // run pre-analysis for scaler
            PointerAnalysisResult preResult = runAnalysis(heapModel,
                    ContextSelectorFactory.makeCISelector());
            selector = Timer.runAndCount(() -> {
                Scaler scaler = new Scaler(preResult);
                return ContextSelectorFactory.makeGuidedSelector(
                        scaler.selectContext());
            }, "Scaler", Level.INFO);
        } else {
            selector = ContextSelectorFactory.makePlainSelector(cs);
        }
        return runAnalysis(heapModel, selector);
    }

    private PointerAnalysisResult runAnalysis(HeapModel heapModel,
                                              ContextSelector selector) {
        AnalysisOptions options = getOptions();
        Solver solver = new DefaultSolver(options,
                heapModel, selector, new MapBasedCSManager());
        // The initialization of some Plugins may read the fields in solver,
        // e.g., contextSelector or csManager, thus we initialize Plugins
        // after setting all other fields of solver.
        setPlugin(solver, options);
        solver.solve();
        return solver.getResult();
    }

    private static void setPlugin(Solver solver, AnalysisOptions options) {
        CompositePlugin plugin = new CompositePlugin();
        // To record elapsed time precisely, AnalysisTimer should be added at first.
        // TODO: remove such order dependency?
        plugin.addPlugin(
                new AnalysisTimer(),
                new ClassInitializer(),
                new ThreadHandler(),
                new ExceptionAnalysis(),
                new ReflectionAnalysis()
        );
        if (World.getOptions().getJavaVersion() < 9) {
            // current reference handler doesn't support Java 9+
            plugin.addPlugin(new ReferenceHandler());
        }
        if (InvokeDynamicAnalysis.useMethodHandle()) {
            plugin.addPlugin(new InvokeDynamicAnalysis());
        }
        if (World.getOptions().getJavaVersion() >= 8) {
            plugin.addPlugin(new LambdaAnalysis());
        }
        if (options.getString("taint-config") != null) {
            plugin.addPlugin(new TaintAnalysis());
        }
        plugin.addPlugin(new ResultProcessor());
        plugin.setSolver(solver);
        solver.setPlugin(plugin);
    }
}
