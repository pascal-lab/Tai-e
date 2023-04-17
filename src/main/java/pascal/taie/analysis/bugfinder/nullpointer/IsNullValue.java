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

package pascal.taie.analysis.bugfinder.nullpointer;

enum IsNullValue {

    /**
     * the variable is definitely null
     */
    NULL(0),

    /**
     * the variable is definitely null due to a null check
     */
    CHECKED_NULL(1),

    /**
     * the variable is definitely not null
     */
    NONNULL(2),

    /**
     * the variable is definitely not null due to a null check
     */
    CHECKED_NN(3),

    /**
     * the variable is definitely not null since it has been dereferenced before
     */
    NO_KABOOM_NN(4),

    /**
     * the variable is null on a simple path, i.e. has a @Check_For_Null annotation
     */
    NSP(5),

    /**
     * nullness of the variable is unknown
     */
    UNKNOWN(6),

    /**
     * the variable is null on a complex path
     */
    NCP(7),

    /**
     * the variable is undefined
     */
    UNDEF(8);

    private final int index;

    IsNullValue(int index) {
        this.index = index;
    }

    public boolean isDefinitelyNull() {
        return this == CHECKED_NULL || this == NULL;
    }

    public boolean isNullOnSomePath() {
        return this == NSP;
    }

    public boolean isAKaBoom() {
        return this == NO_KABOOM_NN;
    }

    public boolean isNullOnComplicatedPath() {
        return this == NCP || this == UNKNOWN;
    }

    public boolean mightBeNull() {
        return isDefinitelyNull() || isNullOnSomePath();
    }

    public boolean isDefinitelyNotNull() {
        return this == NONNULL || this == CHECKED_NN || this == NO_KABOOM_NN;
    }

    private static final IsNullValue[][] mergeMatrix = {
            // NULL, CHECKED_NULL, NN, CHECKED_NN, NO_KABOOM_NN, NSP, UNKNOWN, NCP, UNDEF
            {NULL}, // NULL
            {NULL, CHECKED_NULL}, // CHECKED_NULL
            {NSP, NSP, NONNULL}, // NN
            {NSP, NSP, NONNULL, CHECKED_NN}, // CHECKED_NN
            {NSP, NSP, NONNULL, NONNULL, NO_KABOOM_NN}, // NO_KABOOM_NN
            {NSP, NSP, NSP, NSP, NSP, NSP}, // NSP
            {NSP, NSP, UNKNOWN, UNKNOWN, UNKNOWN, NSP, UNKNOWN}, // UNKNOWN
            {NSP, NSP, NCP, NCP, NCP, NSP, NCP, NCP}, // NCP
            {NULL, CHECKED_NULL, NONNULL, CHECKED_NN, NO_KABOOM_NN, NSP, UNKNOWN, NCP, UNDEF}
    };

    public static IsNullValue merge(IsNullValue a, IsNullValue b) {
        int index1 = a.index;
        int index2 = b.index;
        if (index1 < index2) {
            int tmp = index1;
            index1 = index2;
            index2 = tmp;
        }
        return mergeMatrix[index1][index2];
    }
}
