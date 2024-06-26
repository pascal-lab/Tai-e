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

package pascal.taie.language.classes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class Matcher {

    private final ClassHierarchy hierarchy;

    public Matcher(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public Set<JClass> getClasses(String classPattern) {
        return getClasses(Pattern.parseClassPattern(classPattern));
    }

    Set<JClass> getClasses(Pattern.ClassPattern classPattern) {
        Set<JClass> result = new LinkedHashSet<>();
        Pattern.NamePattern name = classPattern.name();
        if (!name.hasWildcard()) {
            JClass klass = hierarchy.getClass(classPattern.toString());
            if (klass != null) {
                result.add(klass);
            }
        } else {
            // TODO: process name pattern with wildcard
        }
        if (classPattern.includeSubclasses()) {
            new ArrayList<>(result).forEach(c ->
                    result.addAll(hierarchy.getAllSubclassesOf(c)));
        }
        return result;
    }

    public Set<JMethod> getMethods(String methodPattern) {
        return getMethods(Pattern.parseMethodPattern(methodPattern));
    }

    Set<JMethod> getMethods(Pattern.MethodPattern methodPattern) {
        throw new UnsupportedOperationException();
    }

    public Set<JField> getFields(String fieldPattern) {
        return getFields(Pattern.parseFieldPattern(fieldPattern));
    }

    Set<JField> getFields(Pattern.FieldPattern fieldPattern) {
        throw new UnsupportedOperationException();
    }
}
