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

import pascal.taie.java.types.ArrayType;

import java.util.Collections;
import java.util.List;

/**
 * Representation of new multi-array expression, e.g., new T[..][..][..].
 */
public class NewMultiArray extends NewExp {

    private final ArrayType type;

    private final List<Atom> lengths;

    public NewMultiArray(ArrayType type, List<Atom> lengths) {
        this.type = type;
        this.lengths = Collections.unmodifiableList(lengths);
    }

    @Override
    public ArrayType getType() {
        return type;
    }

    public int getLengthCount() {
        return lengths.size();
    }

    public Atom getLength(int i) {
        return lengths.get(i);
    }

    public List<Atom> getLengths() {
        return lengths;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("new ");
        sb.append(type.getBaseType());
        lengths.forEach(length ->
                sb.append('[').append(length).append(']'));
        for (int i = lengths.size(); i < type.getDimensions(); ++i) {
            sb.append("[]");
        }
        return sb.toString();
    }
}
