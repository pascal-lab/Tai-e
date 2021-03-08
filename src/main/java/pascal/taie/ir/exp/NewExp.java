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

package pascal.taie.ir.exp;

import pascal.taie.ir.Site;

/**
 * Representation of new expressions.
 */
public abstract class NewExp implements RValue {

    private Site allocationSite;

    public Site getAllocationSite() {
        return allocationSite;
    }

    public void setAllocationSite(Site allocationSite) {
        this.allocationSite = allocationSite;
    }
}
