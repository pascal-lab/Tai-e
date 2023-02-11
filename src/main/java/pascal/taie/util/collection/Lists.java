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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

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
     * Converts an iterable object to a list.
     */
    public static <T> List<T> asList(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).toList();
    }
}
