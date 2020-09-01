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

package panda.pta.core.solver;

import panda.pta.core.ProgramManager;
import panda.pta.core.context.ContextInsensitiveSelector;
import panda.pta.core.context.OneCallSelector;
import panda.pta.core.context.OneObjectSelector;
import panda.pta.core.context.OneTypeSelector;
import panda.pta.core.context.TwoCallSelector;
import panda.pta.core.context.TwoObjectSelector;
import panda.pta.core.context.TwoTypeSelector;
import panda.pta.core.cs.MapBasedCSManager;
import panda.pta.core.heap.AllocationSiteBasedModel;
import panda.pta.jimple.JimpleProgramManager;
import panda.pta.options.Options;
import panda.pta.plugin.AnalysisTimer;
import panda.pta.plugin.CompositePlugin;
import panda.pta.plugin.Preprocessor;
import panda.pta.plugin.ResultPrinter;
import panda.pta.plugin.ThreadHandler;
import panda.pta.set.HybridPointsToSet;
import panda.pta.set.PointsToSetFactory;
import panda.util.AnalysisException;
import soot.Scene;

public class PointerAnalysisBuilder {

    public PointerAnalysis build(Options options) {
        PointerAnalysisImpl pta = new PointerAnalysisImpl();
        ProgramManager pm = new JimpleProgramManager(Scene.v());
        pta.setProgramManager(pm);
        setContextSensitivity(pta, options);
        setPlugin(pta);
        pta.setHeapModel(new AllocationSiteBasedModel(pm));
        PointsToSetFactory setFactory = new HybridPointsToSet.Factory();
        pta.setPointsToSetFactory(setFactory);
        pta.setCSManager(new MapBasedCSManager(setFactory));
        return pta;
    }

    private void setContextSensitivity(PointerAnalysisImpl pta, Options options) {
        switch (options.getContextSensitivity()) {
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
                                + options.getContextSensitivity());
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
