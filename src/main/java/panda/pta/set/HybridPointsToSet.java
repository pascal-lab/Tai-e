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

package panda.pta.set;

import panda.util.HybridArrayHashSet;

public class HybridPointsToSet extends DelegatePointsToSet {

    @Override
    protected void initializePointsToSet() {
        set = new HybridArrayHashSet<>();
    }

    public static class Factory implements PointsToSetFactory {

        @Override
        public PointsToSet makePointsToSet() {
            return new HybridPointsToSet();
        }
    }
}
