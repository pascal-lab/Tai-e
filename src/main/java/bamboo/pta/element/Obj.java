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

public interface Obj  {

    enum Kind {
        NORMAL,
        STRING_CONSTANT,
        // array, class constants, ...
    }

    Type getType();

    Object getAllocationSite();

    /**
     *
     * @return the method containing the allocation site of this object.
     * Returns null for Some special objects, e.g., string constants,
     * which do not have such method.
     */
    Method getContainerMethod();
}
