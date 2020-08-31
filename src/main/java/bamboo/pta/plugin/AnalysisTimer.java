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

package bamboo.pta.plugin;

import bamboo.util.Timer;

/**
 * Record the elapsed time of pointer analysis.
 */
public class AnalysisTimer implements Plugin {

    private Timer ptaTimer;
    private Timer solverTimer;

    @Override
    public void preprocess() {
        ptaTimer = new Timer("Pointer analysis");
        ptaTimer.start();
    }

    @Override
    public void initialize() {
        solverTimer = new Timer("Pointer analysis solver");
        solverTimer.start();
    }

    @Override
    public void finish() {
        solverTimer.stop();
        ptaTimer.stop();
        System.out.println(solverTimer);
        System.out.println(ptaTimer);
    }
}
