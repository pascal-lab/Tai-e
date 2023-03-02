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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.World;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.StringReps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Streams;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Predicate for checking whether given string constants are
 * reflection-relevant, i.e., class names, method names, or field names.
 */
public class IsReflectionString implements Predicate<String> {

    private final Set<String> reflectionStrings = Sets.newSet(4096);

    public IsReflectionString() {
        World.get().getClassHierarchy().allClasses().forEach(c -> {
            reflectionStrings.add(c.getName());
            Streams.concat(c.getDeclaredMethods().stream(),
                            c.getDeclaredFields().stream())
                    .map(ClassMember::getName)
                    .forEach(reflectionStrings::add);
        });
    }

    @Override
    public boolean test(String s) {
        return (StringReps.isJavaClassName(s) || StringReps.isJavaIdentifier(s))
                && reflectionStrings.contains(s);
    }
}
