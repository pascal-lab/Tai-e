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
            JClass klass = hierarchy.getClass(name.toString());
            if (klass != null) {
                result.add(klass);
            }
        } else {
            // Iterate the whole class hierarchy to find matched classes.
            // This operation MAY cause performance issues.
            Predicate<String> nameMatcher = new NameMatcher(name);
            hierarchy.allClasses()
                    .filter(c -> nameMatcher.test(c.getName()))
                    .forEach(result::add);
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

    private static class NameMatcher implements Predicate<String> {

        private final Predicate<String> matcher;

        private NameMatcher(Pattern.NamePattern pattern) {
            StringBuilder regex = new StringBuilder("^");
            pattern.forEach(unit -> {
                if (unit.equals(Pattern.NAME_WILDCARD)) {
                    regex.append(".*");
                } else {
                    ((Pattern.StringUnit) unit).content()
                            .chars()
                            .forEach(c -> regex.append((c != '.') ? (char) c : "\\."));
                }
            });
            regex.append('$');
            matcher = java.util.regex.Pattern.compile(regex.toString())
                    .asMatchPredicate();
        }

        @Override
        public boolean test(String s) {
            return matcher.test(s);
        }
    }

    }
}
