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

package pascal.taie.util.collection;

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility methods for {@link List}.
 */
public final class Lists {

    private Lists() {
    }

    /**
     * Applies a mapper function on a given collection and returns
     * the results as a list. The resulting list is unmodifiable.
     */
    public static <T, R> List<R> map(Collection<? extends T> c,
                                     Function<T, R> mapper) {
        return c.isEmpty() ? List.of() : c.stream().map(mapper).toList();
    }

    /**
     * Tests the elements in a given collection and returns a list of elements
     * that can pass the test. The resulting list is unmodifiable.
     */
    public static <T> List<T> filter(Collection<T> c,
                                     Predicate<? super T> predicate) {
        List<T> result = c.stream().filter(predicate).toList();
        return result.isEmpty() ? List.of() : result;
    }

    /**
     * Returns a {@link Set} view of the specified list.
     * The set is backed by the list, so changes to the list are reflected in the set.
     * <b>Element uniqueness is the responsibility of the caller:</b>
     * if the list contains duplicates, this set view will not enforce uniqueness
     * and set operations may behave unexpectedly.
     * The set's iteration order is the same as the list's.
     *
     * @param list the list to be viewed as a set
     * @param <T> the type of elements in the list
     * @return a set view of the specified list
     */
    public static <T> Set<T> asSet(List<T> list) {
        return new AbstractSet<>() {
            @Nonnull
            @Override
            public Iterator<T> iterator() {
                return list.iterator();
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }

    /**
     * Concatenates two lists and removes duplicate items in the resulting list.
     */
    public static <T> List<T> concatDistinct(
            List<? extends T> list1, List<? extends T> list2) {
        Set<T> set = new LinkedHashSet<>(list1);
        set.addAll(list2);
        return List.copyOf(set);
    }
}
