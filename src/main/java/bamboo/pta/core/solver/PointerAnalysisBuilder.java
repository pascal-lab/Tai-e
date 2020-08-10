/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.core.solver;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.core.context.ContextInsensitiveSelector;
import bamboo.pta.core.context.OneCallSelector;
import bamboo.pta.core.context.OneObjectSelector;
import bamboo.pta.core.context.OneTypeSelector;
import bamboo.pta.core.context.TwoCallSelector;
import bamboo.pta.core.context.TwoObjectSelector;
import bamboo.pta.core.context.TwoTypeSelector;
import bamboo.pta.core.cs.MapBasedCSManager;
import bamboo.pta.core.heap.AllocationSiteBasedModel;
import bamboo.pta.jimple.JimpleProgramManager;
import bamboo.pta.options.Options;
import bamboo.pta.set.HybridPointsToSet;
import bamboo.pta.set.PointsToSetFactory;
import bamboo.util.AnalysisException;
import soot.Scene;

public class PointerAnalysisBuilder {

    public PointerAnalysis build(Options options) {
        PointerAnalysisImpl pta = new PointerAnalysisImpl();
        ProgramManager pm = new JimpleProgramManager(Scene.v());
        pta.setProgramManager(pm);
        setContextSensitivity(pta, options);
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
}
