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

package panda.pta.element;

import panda.pta.statement.ArrayLoad;
import panda.pta.statement.ArrayStore;
import panda.pta.statement.Call;
import panda.pta.statement.InstanceLoad;
import panda.pta.statement.InstanceStore;

import java.util.Set;

public interface Variable {

    Type getType();

    Method getContainerMethod();

    String getName();

    /**
     * Adds a call whose receiver variable is this variable.
     */
    void addCall(Call call);

    /**
     * @return set of call statements where this variable is the receiver.
     */
    Set<Call> getCalls();

    /**
     * Adds an instance load whose base variable is this variable.
     */
    void addInstanceLoad(InstanceLoad load);

    /**
     * @return set of instance loads where this variable is the base.
     */
    Set<InstanceLoad> getInstanceLoads();

    /**
     * Adds an instance store whose base variable is this variable.
     */
    void addInstanceStore(InstanceStore store);

    /**
     * @return set of instance stores where this variable is the base.
     */
    Set<InstanceStore> getInstanceStores();

    /**
     * Adds an array load whose base variable is this variable.
     */
    void addArrayLoad(ArrayLoad load);

    /**
     * @return set of array loads where this variable is the base.
     */
    Set<ArrayLoad> getArrayLoads();

    /**
     * Adds an array store whose base variable is this variable.
     */
    void addArrayStore(ArrayStore store);

    /**
     * @return set of array stores where this variable is the base.
     */
    Set<ArrayStore> getArrayStores();
}
