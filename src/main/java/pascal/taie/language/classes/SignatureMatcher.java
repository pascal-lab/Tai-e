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

import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Provides functionality to match signatures by given patterns.
 */
public class SignatureMatcher {

    private final ClassHierarchy hierarchy;

    public SignatureMatcher(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    /**
     * @return the classes that match given pattern.
     */
    public Set<JClass> getClasses(String classPattern) {
        return getClasses(Pattern.parseClassPattern(classPattern));
    }

    private Set<JClass> getClasses(Pattern.ClassPattern classPattern) {
        Set<JClass> result = Sets.newLinkedSet();
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

    /**
     * @return the methods that match given pattern.
     */
    public Set<JMethod> getMethods(String methodPattern) {
        Pattern.MethodPattern pattern = Pattern.parseMethodPattern(methodPattern);
        Set<JMethod> result = Sets.newLinkedSet();
        if (pattern.isExactMatch()) {
            JMethod method = hierarchy.getMethod(pattern.toString());
            if (method != null) {
                result.add(method);
            }
        } else {
            Predicate<Type> typeMatcher = new TypeMatcher(pattern.retType());
            Predicate<String> nameMatcher = new NameMatcher(pattern.name());
            Predicate<List<Type>> paramsMatcher = new ParamsMatcher(pattern.params());
            getClasses(pattern.klass())
                    .stream()
                    .map(JClass::getDeclaredMethods)
                    .flatMap(Collection::stream)
                    .filter(method -> typeMatcher.test(method.getReturnType())
                            && nameMatcher.test(method.getName())
                            && paramsMatcher.test(method.getParamTypes()))
                    .forEach(result::add);
        }
        return result;
    }

    /**
     * @return the fields that match given pattern.
     */
    public Set<JField> getFields(String fieldPattern) {
        Pattern.FieldPattern pattern = Pattern.parseFieldPattern(fieldPattern);
        Set<JField> result = Sets.newLinkedSet();
        if (pattern.isExactMatch()) {
            JField field = hierarchy.getField(pattern.toString());
            if (field != null) {
                result.add(field);
            }
        } else {
            Predicate<Type> typeMatcher = new TypeMatcher(pattern.type());
            Predicate<String> nameMatcher = new NameMatcher(pattern.name());
            getClasses(pattern.klass())
                    .stream()
                    .map(JClass::getDeclaredFields)
                    .flatMap(Collection::stream)
                    .filter(field -> typeMatcher.test(field.getType())
                            && nameMatcher.test(field.getName()))
                    .forEach(result::add);
        }
        return result;
    }

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

    /**
     * If type pattern includes subtypes, we only consider class types.
     */
    private class TypeMatcher implements Predicate<Type> {

        private final boolean includeSubtypes;

        private final Set<JClass> superClasses;

        private final NameMatcher matcher;

        private TypeMatcher(Pattern.TypePattern pattern) {
            includeSubtypes = pattern.includeSubtypes();
            if (includeSubtypes) {
                superClasses = getClasses(pattern.name().toString());
                matcher = null;
            } else {
                superClasses = null;
                matcher = new NameMatcher(pattern.name());
            }
        }

        @Override
        public boolean test(Type type) {
            if (includeSubtypes) {
                if (type instanceof ClassType classType) {
                    JClass klass = classType.getJClass();
                    return superClasses.stream()
                            .anyMatch(c -> hierarchy.isSubclass(c, klass));
                } else {
                    return false;
                }
            } else {
                return matcher.test(type.getName());
            }
        }
    }

    private class ParamsMatcher implements Predicate<List<Type>> {

        private final List<Pattern.ParamUnit> units;

        private final Map<Pattern.TypePattern, TypeMatcher> typeMatchers;

        private ParamsMatcher(List<Pattern.ParamUnit> units) {
            this.units = units;
            this.typeMatchers = units.stream()
                    .map(Pattern.ParamUnit::type)
                    .collect(Collectors.toMap(
                            tp -> tp,
                            TypeMatcher::new,
                            (tm1, tm2) -> tm1));
        }

        @Override
        public boolean test(List<Type> params) {
            return matches(0, params, 0);
        }

        private boolean matches(int unitIndex, List<Type> params, int paramIndex) {
            if (unitIndex == units.size()) { // all units have been matched
                return paramIndex == params.size();
            }
            Pattern.ParamUnit currentUnit = units.get(unitIndex);
            TypeMatcher typeMatcher = typeMatchers.get(currentUnit.type());
            Pattern.Repeat repeat = currentUnit.repeat();
            // iterate over times of repetition
            for (int count = repeat.min(); count <= repeat.max(); ++count) {
                // compute the range of parameters to be matched by currentUnit
                // i.e., [paramIndex, nextParamIndex]
                int nextParamIndex = paramIndex + count;
                if (nextParamIndex > params.size()) {
                    // exceed params, no need for further iteration
                    break;
                }
                // matches currentUnit and parameter types
                // in [paramIndex, nextParamIndex]
                boolean match = true;
                for (int i = paramIndex; i < nextParamIndex; ++i) {
                    if (!typeMatcher.test(params.get(i))) {
                        match = false;
                        break;
                    }
                }
                // currentUnit and [paramIndex, nextParamIndex] match,
                // skip currentUnit and try to match the rest of units
                if (match && matches(unitIndex + 1, params, nextParamIndex)) {
                    return true;
                }
            }
            return false;
        }
    }
}
