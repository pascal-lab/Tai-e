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

package pascal.taie.util;

import java.util.function.Predicate;

public final class Predicates {

    private Predicates() {
    }

    private enum PresetPredicates implements Predicate<Object> {

        ALWAYS_TRUE {
            @Override
            public boolean test(Object o) {
                return true;
            }

            @Override
            public String toString() {
                return "Predicates.alwaysTrue()";
            }
        },

        ALWAYS_FALSE {
            @Override
            public boolean test(Object o) {
                return false;
            }

            @Override
            public String toString() {
                return "Predicates.alwaysFalse()";
            }
        };

        @SuppressWarnings("unchecked") // safe contravariant cast
        private <T> Predicate<T> withNarrowedType() {
            return (Predicate<T>) this;
        }
    }

    /**
     * @return a predicate that always evaluates to {@code true}.
     */
    public static <T> Predicate<T> alwaysTrue() {
        return PresetPredicates.ALWAYS_TRUE.withNarrowedType();
    }

    /**
     * @return a predicate that always evaluates to {@code false}.
     */
    public static <T> Predicate<T> alwaysFalse() {
        return PresetPredicates.ALWAYS_FALSE.withNarrowedType();
    }
}
