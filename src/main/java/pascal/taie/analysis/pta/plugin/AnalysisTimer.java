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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.util.Timer;

/**
 * Records the elapsed time of pointer analysis.
 */
public class AnalysisTimer implements Plugin {

    private Timer ptaTimer;
    private Timer solverTimer;

    @Override
    public void onPreprocess() {
        ptaTimer = new Timer("Pointer analysis");
        ptaTimer.start();
    }

    @Override
    public void onInitialize() {
        solverTimer = new Timer("Pointer analysis solver");
        solverTimer.start();
    }

    @Override
    public void onFinish() {
        solverTimer.stop();
        ptaTimer.stop();
        System.out.println(solverTimer);
        System.out.println(ptaTimer);
    }
}
