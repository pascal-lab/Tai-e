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

package bamboo.pta.monitor;

import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.element.Method;
import bamboo.pta.set.PointsToSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite monitor which allows multiple independent monitors
 * to be used together.
 */
public class CompositeMonitor implements AnalysisMonitor {

    private final List<AnalysisMonitor> monitors = new ArrayList<>();

    public void addMonitor(AnalysisMonitor monitor) {
        monitors.add(monitor);
    }

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        monitors.forEach(m -> m.setPointerAnalysis(pta));
    }

    @Override
    public void signalInitialization() {
        monitors.forEach(AnalysisMonitor::signalInitialization);
    }

    @Override
    public void signalFinish() {
        monitors.forEach(AnalysisMonitor::signalFinish);
    }

    @Override
    public void signalNewPointsToSet(CSVariable csVar, PointsToSet pts) {
        monitors.forEach(m -> m.signalNewPointsToSet(csVar, pts));
    }

    @Override
    public void signalNewMethod(Method method) {
        monitors.forEach(m -> m.signalNewMethod(method));
    }
}
