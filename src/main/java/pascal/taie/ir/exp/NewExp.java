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

package pascal.taie.ir.exp;

import pascal.taie.ir.proginfo.ProgramPoint;

/**
 * Representation of new expressions.
 */
public abstract class NewExp implements RValue {

    private ProgramPoint allocationSite;

    public ProgramPoint getAllocationSite() {
        return allocationSite;
    }

    public void setAllocationSite(ProgramPoint allocationSite) {
        this.allocationSite = allocationSite;
    }
}
