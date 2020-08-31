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

import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.element.Method;
import bamboo.pta.set.PointsToSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite plugin which allows multiple independent plugins
 * to be used together.
 */
public class CompositePlugin implements Plugin {

    private final List<Plugin> plugins = new ArrayList<>();

    public void addPlugin(Plugin plugin) {
        plugins.add(plugin);
    }

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        plugins.forEach(p -> p.setPointerAnalysis(pta));
    }

    @Override
    public void preprocess() {
        plugins.forEach(Plugin::preprocess);
    }

    @Override
    public void initialize() {
        plugins.forEach(Plugin::initialize);
    }

    @Override
    public void finish() {
        plugins.forEach(Plugin::finish);
    }

    @Override
    public void postprocess() {
        plugins.forEach(Plugin::postprocess);
    }

    @Override
    public void handleNewPointsToSet(CSVariable csVar, PointsToSet pts) {
        plugins.forEach(p -> p.handleNewPointsToSet(csVar, pts));
    }

    @Override
    public void handleNewMethod(Method method) {
        plugins.forEach(p -> p.handleNewMethod(method));
    }

    @Override
    public void handleNewCSMethod(CSMethod csMethod) {
        plugins.forEach(p -> p.handleNewCSMethod(csMethod));
    }
}
