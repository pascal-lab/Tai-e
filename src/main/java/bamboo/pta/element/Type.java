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

import java.util.Optional;
import java.util.Set;

public interface Type {

    String getName();

    /**
     * @return if this type is class type.
     */
    boolean isClassType();

    /**
     * @return if this type is array type.
     */
    boolean isArrayType();

    /**
     * @return the direct super class of this type.
     */
    Optional<Type> getSuperClass();

    /**
     * @return the direct super interfaces of this type.
     */
    Set<Type> getSuperInterfaces();

    /**
     * @return the element type if this type is array type.
     */
    Type getElementType();

    /**
     * @return the base element type if this type is array type.
     */
    Type getBaseType();

    /**
     * @return the class initializer of this type.
     */
    Optional<Method> getClassInitializer();
}
