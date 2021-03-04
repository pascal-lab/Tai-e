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

package pascal.taie.ir;

import pascal.taie.java.World;
import pascal.taie.java.types.PrimitiveType;

public class LongLiteral implements IntegerLiteral {

    private static PrimitiveType type;

    /**
     * Cache frequently used literals for saving space.
     */
    private static final LongLiteral[] cache = new LongLiteral[-(-128) + 127 + 1];

    static {
        for(int i = 0; i < cache.length; i++)
            cache[i] = new LongLiteral(i - 128);
    }

    private final long value;

    private LongLiteral(long value) {
        this.value = value;
    }

    public static LongLiteral get(long value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // will cache
            return cache[(int)value + offset];
        }
        return new LongLiteral(value);
    }

    @Override
    public PrimitiveType getType() {
        if (type == null) {
            type = World.get()
                    .getTypeManager()
                    .getLongType();
        }
        return type;
    }

    public long getValue() {
        return value;
    }

}
