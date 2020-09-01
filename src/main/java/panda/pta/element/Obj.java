/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.element;

import java.util.Optional;

public interface Obj {

    Kind getKind();

    Type getType();

    /**
     * For NORMAL, returns the allocation site.
     * For STRING_CONSTANT, returns the string constant.
     * For CLASS/METHOD/FIELD/CONSTRUCTOR, returns the corresponding
     * class/method/field/constructor.
     * For MERGED, ENV and ARTIFICIAL, the return value
     * depends on concrete implementation.
     */
    Object getAllocation();

    /**
     * @return the method containing the allocation site of this object.
     * For Some special objects, e.g., string constants do not
     * have such method.
     */
    Optional<Method> getContainerMethod();

    /**
     * This method is useful for type sensitivity.
     *
     * @return the type containing the allocation site of this object.
     * For special objects, the return values of this method are also special.
     */
    Type getContainerType();

    enum Kind {
        NORMAL, // normal objects created by allocation sites
        STRING_CONSTANT, // string constants

        CLASS, // objects of java.lang.Class
        METHOD, // objects of java.lang.reflect.Method
        FIELD, // objects of java.lang.reflect.Field
        CONSTRUCTOR, // objects of java.lang.reflect.Constructor

        REFLECTIVE_OBJECT, // reflectively-created objects

        MERGED, // represents merged objects (heap sensitivity
        // is not applied to these objects)
        ENV, // represented objects created or controlled by Java
        // runtime environment (heap sensitivity is also not applied)
        ARTIFICIAL, // represents the non-exist objects
    }
}
