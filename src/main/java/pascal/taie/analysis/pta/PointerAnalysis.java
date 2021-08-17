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
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.pta.core.solver.DefaultSolver;
import pascal.taie.analysis.pta.core.solver.SimpleSolver;
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
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.ConfigException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class PointerAnalysis extends InterproceduralAnalysis {

    public static final String ID = "pta";

    public PointerAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public PointerAnalysisResult analyze() {
        Solver solver = newSolver();
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

    private Solver newSolver() {
        switch (getOptions().getString("solver")) {
            case "default": return new DefaultSolver();
            case "simple": return new SimpleSolver();
            default: throw new ConfigException("Unknown solver: " +
                    getOptions().getString("solver"));
        }
    }

    private void setContextSensitivity(Solver solver) {
        String cs = getOptions().getString("cs");
        if (cs.equals("ci")) {
            solver.setContextSelector(new ContextInsensitiveSelector());
        } else {
            try {
                // we expect that the argument of context-sensitivity variant
                // is of pattern k-kind, where k is limit of context length
                // and kind represents kind of context element (obj, type, etc.).
                String[] splits = cs.split("-");
                int k = Integer.parseInt(splits[0]);
                String kind = splits[1].substring(0, 1).toUpperCase() +
                        splits[1].substring(1).toLowerCase();
                String selectorName = "pascal.taie.analysis.pta.core.cs.selector." +
                        "K" + kind + "Selector";
                Class<?> c = Class.forName(selectorName);
                Constructor<?> ctor = c.getConstructor(int.class);
                ContextSelector selector = (ContextSelector) ctor.newInstance(k);
                solver.setContextSelector(selector);
            } catch (RuntimeException e) {
                throw new ConfigException("Unexpected context-sensitivity variants: " + cs, e);
            } catch (ClassNotFoundException | NoSuchMethodException |
                    InvocationTargetException | InstantiationException |
                    IllegalAccessException e) {
                throw new ConfigException("Failed to initialize context selector: " + cs, e);
            }
        }
    }

    private void setPlugin(Solver solver) {
        CompositePlugin plugin = new CompositePlugin();
        // To record elapsed time precisely, AnalysisTimer should be added at first.
        // TODO: remove such order dependency
        plugin.addPlugin(
                new AnalysisTimer(),
                new ClassInitializer(),
                new ThreadHandler(),
                new ExceptionAnalysis(),
                new ReflectionAnalysis(),
                new ResultProcessor()
        );
        if (World.getOptions().getJavaVersion() < 9) {
            // current reference handler doesn't support Java 9+
            plugin.addPlugin(new ReferenceHandler());
        }
        if (World.getOptions().getJavaVersion() >= 7) {
            plugin.addPlugin(new InvokeDynamicAnalysis());
        }
        if (World.getOptions().getJavaVersion() >= 8) {
            plugin.addPlugin(new LambdaAnalysis());
        }
        if (getOptions().getString("taint-config") != null) {
            plugin.addPlugin(new TaintAnalysis());
        }
        plugin.setSolver(solver);
        solver.setPlugin(plugin);
    }
}
