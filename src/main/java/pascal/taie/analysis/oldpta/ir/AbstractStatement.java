/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.oldpta.ir;

public abstract class AbstractStatement implements Statement {

    private int startLineNumber = -1;

    @Override
    public int getStartLineNumber() {
        return startLineNumber;
    }

    @Override
    public void setStartLineNumber(int startLineNumber) {
        this.startLineNumber = startLineNumber;
    }
}
