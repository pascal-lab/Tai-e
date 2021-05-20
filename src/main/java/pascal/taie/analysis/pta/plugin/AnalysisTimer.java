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

    @Override
    public void onStart() {
        ptaTimer = new Timer("Pointer analysis");
        ptaTimer.start();
    }

    @Override
    public void onFinish() {
        ptaTimer.stop();
        System.out.println(ptaTimer);
    }
}
