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

import pascal.taie.World;
import pascal.taie.analysis.pta.core.context.ContextInsensitiveSelector;
import pascal.taie.analysis.pta.core.context.KCallSelector;
import pascal.taie.analysis.pta.core.context.KObjSelector;
import pascal.taie.analysis.pta.core.context.KTypeSelector;
import pascal.taie.analysis.pta.PTAOptions;
import pascal.taie.analysis.pta.core.cs.MapBasedCSManager;
import pascal.taie.analysis.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.pta.plugin.AnalysisTimer;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.Preprocessor;
import pascal.taie.analysis.pta.plugin.ReferenceHandler;
import pascal.taie.analysis.pta.plugin.ResultPrinter;
import pascal.taie.analysis.pta.plugin.ThreadHandler;
import pascal.taie.analysis.pta.set.HybridPointsToSet;
import pascal.taie.analysis.pta.set.PointsToSetFactory;
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
                new ReferenceHandler(),
                ResultPrinter.get()
        );
        plugin.setPointerAnalysis(pta);
        pta.setPlugin(plugin);
    }
}
