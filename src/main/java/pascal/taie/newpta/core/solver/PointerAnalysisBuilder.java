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

package pascal.taie.newpta.core.solver;

import pascal.taie.java.World;
import pascal.taie.newpta.core.context.ContextInsensitiveSelector;
import pascal.taie.newpta.core.context.KCallSelector;
import pascal.taie.newpta.core.context.KObjSelector;
import pascal.taie.newpta.core.context.KTypeSelector;
import pascal.taie.newpta.core.cs.MapBasedCSManager;
import pascal.taie.newpta.core.heap.AllocationSiteBasedModel;
import pascal.taie.newpta.plugin.AnalysisTimer;
import pascal.taie.newpta.plugin.CompositePlugin;
import pascal.taie.newpta.plugin.Preprocessor;
import pascal.taie.newpta.plugin.ResultPrinter;
import pascal.taie.newpta.plugin.ThreadHandler;
import pascal.taie.newpta.set.HybridPointsToSet;
import pascal.taie.newpta.set.PointsToSetFactory;
import pascal.taie.newpta.PTAOptions;
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
                pta.setContextSelector(new KCallSelector(1));
                break;
            case "1-obj":
            case "1-object":
                pta.setContextSelector(new KObjSelector(1));
                break;
            case "1-type":
                pta.setContextSelector(new KTypeSelector(1));
                break;
            case "2-call":
            case "2-cfa":
                pta.setContextSelector(new KCallSelector(2));
                break;
            case "2-obj":
            case "2-object":
                pta.setContextSelector(new KObjSelector(2));
                break;
            case "2-type":
                pta.setContextSelector(new KTypeSelector(2));
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
                ResultPrinter.get()
        );
        plugin.setPointerAnalysis(pta);
        pta.setPlugin(plugin);
    }
}
