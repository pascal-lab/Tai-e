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

package pascal.taie.newpta.core.heap;

import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;

import java.util.Optional;

public interface Obj {

    Type getType();

    Object getAllocation();

    /**
     * @return the method containing the allocation site of this object.
     * For Some special objects, e.g., string constants do not
     * have such method.
     */
    Optional<JMethod> getContainerMethod();

    /**
     * This method is useful for type sensitivity.
     *
     * @return the type containing the allocation site of this object.
     * For special objects, the return values of this method are also special.
     */
    Type getContainerType();
}
