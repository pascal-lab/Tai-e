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

import pascal.taie.language.type.Type;

public interface JValue {

    Object toJVMObj();

    Type getType();

    static int getInt(JValue v) {
        if (v instanceof JPrimitive l &&
            l.value instanceof Integer i) {
            return i;
        } else {
            throw new VMException(v + " is not int value");
        }
    }

    static long getLong(JValue v) {
        if (v instanceof JPrimitive l &&
                l.value instanceof Long i) {
            return i;
        } else {
            throw new VMException(v + " is not long value");
        }
    }

    static JArray getJArray(JValue v) {
        if (v instanceof JArray j) {
            return j;
        } else {
            throw new VMException(v + " is not array value");
        }
    }

    static JObject getObject(JValue v) {
        if (v instanceof JObject j) {
            return j;
        } else {
            throw new VMException(v + " is not Object value");
        }
    }
}
