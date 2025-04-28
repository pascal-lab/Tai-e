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

import pascal.taie.World;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a Java array.
 */
public class JArray implements JValue {
    JValue[] arr;

    final ArrayType type;

    public JArray(JValue[] arr, Type baseType, int dims) {
        this.type = World.get().getTypeSystem().getArrayType(baseType, dims);
        this.arr = arr;
    }

    public JValue getIdx(int idx) {
        return arr[idx];
    }

    public void setIdx(int idx, JValue v) {
        arr[idx] = v;
    }

    public int length() {
        return arr.length;
    }

    public static JArray createArray(int count, Type baseType, int dims) {
        JArray arr = new JArray(new JValue[count], baseType, dims);
        JValue ele = dims == 1 && baseType instanceof PrimitiveType t ?
                JPrimitive.getDefault(t) : JNull.NULL;
        for (int i = 0; i < count; ++i) {
            arr.setIdx(i, ele);
        }
        return arr;
    }

    public static JArray createMultiArray(ArrayType at, List<Integer> counts, int countIdx) {
        int now = counts.get(countIdx);
        JArray arr = createArray(now, at.baseType(), at.dimensions());
        if (countIdx + 1 < counts.size()) {
            for (int i = 0; i < now; ++i) {
                Type eleType = at.elementType();
                JValue ele = eleType instanceof ArrayType eleAt ?
                        createMultiArray(eleAt, counts, countIdx + 1) :
                        createArray(counts.get(countIdx + 1), eleType, 1);
                arr.setIdx(i, ele);
            }
        }
        return arr;
    }

    @Override
    public String toString() {
        return "JArray " + Arrays.toString(arr);
    }

    @Override
    public Object toJVMObj() {
        Class<?> klass = Utils.toJVMType(type);
        Object res = Array.newInstance(klass.getComponentType(), this.arr.length);
        for (int i = 0; i < arr.length; ++i) {
            // TODO: warning when try to convert from non-jvm objects
            Array.set(res, i, Utils.typedToJVMObj(arr[i], type.elementType()));
        }
        return res;
    }

    @Override
    public Type getType() {
        return type;
    }

    public JArray(JArray another)  {
        this(another.arr.clone(), another.type.baseType(), another.type.dimensions());
    }

    public void update(JArray another) {
        this.arr = another.arr;
    }

}
