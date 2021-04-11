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

    public PointerAnalysis build(Options options) {
        PointsToSetFactory.setFactory(new HybridPointsToSet.Factory());
        PointerAnalysisImpl pta = new PointerAnalysisImpl();
        setContextSensitivity(pta, options);
        setPlugin(pta);
        pta.setHeapModel(new AllocationSiteBasedModel(
                World.getTypeManager()));
        pta.setCSManager(new MapBasedCSManager());
        return pta;
    }

    private void setContextSensitivity(PointerAnalysisImpl pta, Options options) {
        switch (options.getContextSensitivity()) {
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
                new ThreadHandler(),
                new ReferenceHandler(),
                ResultPrinter.get()
        );
        plugin.setPointerAnalysis(pta);
        pta.setPlugin(plugin);
    }
}
