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

package bamboo.pta.element;

import bamboo.pta.statement.Call;
import bamboo.pta.statement.InstanceLoad;
import bamboo.pta.statement.InstanceStore;

import java.util.Set;

public interface Variable {

    Type getType();

    Method getContainerMethod();

    String getName();

    /**
     *
     * @return set of call statements where this variable is the receiver.
     */
    Set<Call> getCalls();

    /**
     *
     * @return set of instance loads where this variable is the base.
     */
    Set<InstanceLoad> getLoads();

    /**
     *
     * @return set of instance stores where this variable is the base.
     */
    Set<InstanceStore> getStores();
}
