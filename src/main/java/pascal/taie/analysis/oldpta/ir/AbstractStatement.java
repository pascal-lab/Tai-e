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
