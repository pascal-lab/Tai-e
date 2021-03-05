/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.solver;

import pascal.taie.java.World;
import pascal.taie.pta.PTAOptions;
import pascal.taie.pta.core.context.ContextInsensitiveSelector;
import pascal.taie.pta.core.context.OneCallSelector;
import pascal.taie.pta.core.context.OneObjectSelector;
import pascal.taie.pta.core.context.OneTypeSelector;
import pascal.taie.pta.core.context.TwoCallSelector;
import pascal.taie.pta.core.context.TwoObjectSelector;
import pascal.taie.pta.core.context.TwoTypeSelector;
import pascal.taie.pta.core.cs.MapBasedCSManager;
import pascal.taie.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.pta.plugin.AnalysisTimer;
import pascal.taie.pta.plugin.CompositePlugin;
import pascal.taie.pta.plugin.Preprocessor;
import pascal.taie.pta.plugin.ResultPrinter;
import pascal.taie.pta.plugin.ThreadHandler;
import pascal.taie.pta.set.HybridPointsToSet;
import pascal.taie.pta.set.PointsToSetFactory;
import pascal.taie.util.AnalysisException;

public class PointerAnalysisBuilder {

    public PointerAnalysis build(PTAOptions PTAOptions) {
        PointsToSetFactory.setFactory(new HybridPointsToSet.Factory());
        PointerAnalysisImpl pta = new PointerAnalysisImpl();
        setContextSensitivity(pta, PTAOptions);
        setPlugin(pta);
        pta.setHeapModel(new AllocationSiteBasedModel(
                World.getTypeManager()));
        pta.setCSManager(new MapBasedCSManager());
        return pta;
    }

    private void setContextSensitivity(PointerAnalysisImpl pta, PTAOptions PTAOptions) {
        switch (PTAOptions.getContextSensitivity()) {
            case "ci":
                pta.setContextSelector(new ContextInsensitiveSelector());
                break;
            case "1-call":
            case "1-cfa":
                pta.setContextSelector(new OneCallSelector());
                break;
            case "1-obj":
            case "1-object":
                pta.setContextSelector(new OneObjectSelector());
                break;
            case "1-type":
                pta.setContextSelector(new OneTypeSelector());
                break;
            case "2-call":
            case "2-cfa":
                pta.setContextSelector(new TwoCallSelector());
                break;
            case "2-obj":
            case "2-object":
                pta.setContextSelector(new TwoObjectSelector());
                break;
            case "2-type":
                pta.setContextSelector(new TwoTypeSelector());
                break;
            default:
                throw new AnalysisException(
                        "Unknown context sensitivity variant: "
                                + PTAOptions.getContextSensitivity());
        }
    }

    private void setPlugin(PointerAnalysisImpl pta) {
        CompositePlugin plugin = new CompositePlugin();
        // To record elapsed time precisely, AnalysisTimer should be
        // added at first.
        // TODO: remove such order dependency
        plugin.addPlugin(
                new AnalysisTimer(),
                new Preprocessor(),
                new ThreadHandler(),
                ResultPrinter.v()
        );
        plugin.setPointerAnalysis(pta);
        pta.setPlugin(plugin);
    }
}
