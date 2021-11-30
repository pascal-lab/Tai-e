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

package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.cs.Solver;

import java.util.ArrayList;
import java.util.List;

public class TaintAnalysiss {

    private static final Logger logger = LogManager.getLogger(TaintAnalysiss.class);

    private final TaintManager manager;

    private final TaintConfig config;

    private final Solver solver;

    private final CSManager csManager;

    private final Context emptyContext;

    public TaintAnalysiss(Solver solver) {
        manager = new TaintManager();
        this.solver = solver;
        csManager = solver.getCSManager();
        emptyContext = solver.getContextSelector().getEmptyContext();
        config = TaintConfig.readConfig(
                solver.getOptions().getString("taint-config"),
                World.getClassHierarchy(),
                World.getTypeManager());
        logger.info(config);
    }

    // TODO - finish me

    public void onFinish() {
        List<TaintFlow> taintFlows = collectTaintFlows();
        solver.getResult().storeResult(getClass().getName(), taintFlows);
    }

    private List<TaintFlow> collectTaintFlows() {
        List<TaintFlow> taintFlows = new ArrayList<>();
        PointerAnalysisResult result = solver.getResult();
        // TODO - finish me
        // You could query pointer analysis results you need via variable result.
        return taintFlows;
    }
}
