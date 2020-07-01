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

package bamboo.pta.env;

import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;

class StringConstant implements Obj {

    private final Type type;

    private final String value;

    StringConstant(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object getAllocation() {
        return value;
    }

    @Override
    public Method getContainerMethod() {
        // String constants do not have a container method, as the same
        // string constant can appear in multiple methods.
        return null;
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
