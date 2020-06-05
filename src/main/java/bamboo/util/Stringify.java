/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.util;

import bamboo.pta.element.Obj;
import soot.jimple.AssignStmt;

import java.util.function.Function;
import java.util.stream.Stream;

public class Stringify {

    public static <T> String streamToString(
            Stream<T> stream, Function<T, String> toString) {
        Iterable<String> elems = () -> stream.map(toString)
                .sorted()
                .iterator();
        return "{" + String.join(",", elems) + "}";
    }
}
