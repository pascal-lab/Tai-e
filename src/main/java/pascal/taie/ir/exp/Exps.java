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

package pascal.taie.ir.exp;

import pascal.taie.language.type.LongType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;

/**
 * Provides static utility methods for {@link Exp}.
 */
public final class Exps {

    private Exps() {
    }

    /**
     * @return {@code true} if {@code exp} can hold primitive values.
     */
    public static boolean holdsPrimitive(Exp exp) {
        return exp.getType() instanceof PrimitiveType;
    }

    /**
     * @return {@code true} if {@code exp} can hold int values.
     * Note that expressions of some primitive types other than int,
     * whose computational type is int, can also hold int values.
     * @see PrimitiveType#asInt()
     */
    public static boolean holdsInt(Exp exp) {
        return exp.getType() instanceof PrimitiveType t && t.asInt();
    }

    /**
     * @return {@code true} if {@code exp} can hold long values.
     */
    public static boolean holdsLong(Exp exp) {
        return exp.getType().equals(LongType.LONG);
    }

    /**
     * @return {@code true} if {@code exp} can hold int or long values.
     */
    public static boolean holdsInteger(Exp exp) {
        return holdsInt(exp) || holdsLong(exp);
    }

    /**
     * @return {@code true} if {@code exp} can hold reference values.
     */
    public static boolean holdsReference(Exp exp) {
        // TODO: exclude null type?
        return exp.getType() instanceof ReferenceType;
    }
}
