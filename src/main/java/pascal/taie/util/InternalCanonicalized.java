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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marker annotation.
 * If a class is annotated by this annotation, it means that the class
 * applies internal canonicalization and the instances of the class
 * are canonicalized by the class internally.
 * <p>
 * The annotated classes use the following pattern to canonicalize
 * their instances:
 * <ol>
 *     <li>use default equals() and hashCode() (allow fast comparison)
 *     <li>create a private class named Key, which overrides equals() and hashCode()
 *     <li>maintain a hash map from Key to instance
 *     <li>hide constructor, and provide static factory method for
 *     obtaining instances, and use the map for canonicalization
 * </ol>
 */
@Target(ElementType.TYPE)
public @interface InternalCanonicalized {
}
