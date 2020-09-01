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

package panda.pta.core.context;

public interface Context {

    /**
     * @return the depth (i.e., the number of elements) of this context.
     */
    int depth();

    /**
     * @return the i-th element of this context. Starts from 1.
     */
    Object element(int i);
}
