/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.vm;

import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;

import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;

/**
 * This class represents a primitive value in the VM.
 */
public class JPrimitive implements JValue {

    final Object value;

    final PrimitiveType type;

    public JPrimitive(Object v) {
        assert v instanceof Integer ||
                v instanceof Long ||
                v instanceof Float ||
                v instanceof Double;

        this.value = v;

        if (value instanceof Integer) {
            type = INT;
        } else if (value instanceof Long) {
            type = LONG;
        } else if (value instanceof Float) {
            type = FLOAT;
        } else {
            type = DOUBLE;
        }
    }

    public static JPrimitive get(Object value) {
        return new JPrimitive(value);
    }

    public JPrimitive getNegValue() {
        if (value instanceof Integer i) {
            return get(-i);
        } else if (value instanceof Long l) {
            return get(-l);
        } else if (value instanceof Float f) {
            return get(-f);
        } else if (value instanceof Double d) {
            return get(-d);
        } else {
            throw new VMException();
        }
    }

    public static JPrimitive getBoolean(boolean b) {
        return new JPrimitive(Utils.toInt(b));
    }

    public static JPrimitive getDefault(PrimitiveType t) {
        int index = pascal.taie.frontend.newfrontend.Utils.getPrimitiveTypeIndex(t);
        return switch (index) {
            case 0, 1, 2, 3, 4 -> JPrimitive.get(0);
            case 5 -> JPrimitive.get(0L);
            case 6 -> JPrimitive.get(0f);
            case 7 -> JPrimitive.get(0d);
            default -> throw new VMException();
        };
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public Object toJVMObj() {
        return value;
    }

    @Override
    public Type getType() {
        return type;
    }
}
