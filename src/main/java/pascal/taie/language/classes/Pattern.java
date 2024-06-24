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

import pascal.taie.util.Hashes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pattern representation and parsing.
 */
class Pattern {

    static final String SUB_MARK = "^";

    static final String FULLNAME_WILDCARD_MARK = "**";

    static final String NAME_WILDCARD_MARK = "*";

    static final String PARAM_WILDCARD_MARK = "~";

    /**
     * ClassPattern -> NamePattern[^]
     */
    static ClassPattern parseClassPattern(String pattern) {
        boolean includeSubclasses;
        if (pattern.endsWith(SUB_MARK)) {
            includeSubclasses = true;
            pattern = pattern.substring(0, pattern.length() - 1);
        } else {
            includeSubclasses = false;
        }
        return new ClassPattern(parseNamePattern(pattern), includeSubclasses);
    }

    static class ClassPattern {

        private final NamePattern name;

        private final boolean includeSubclasses;

        ClassPattern(NamePattern name, boolean includeSubclasses) {
            this.name = name;
            this.includeSubclasses = includeSubclasses;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ClassPattern other)) {
                return false;
            }
            return name.equals(other.name)
                    && includeSubclasses == other.includeSubclasses;
        }

        @Override
        public int hashCode() {
            return Hashes.hash(name, includeSubclasses);
        }

        @Override
        public String toString() {
            return includeSubclasses ? name + SUB_MARK : name.toString();
        }
    }

    static NamePattern parseNamePattern(String pattern) {
        List<NameUnit> units = new ArrayList<>();
        int i = 0, lastI = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (lastI < i) { // match string
                    units.add(new StringUnit(pattern.substring(lastI, i)));
                }
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') { // match **
                    units.add(FULLNAME_WILDCARD);
                    ++i;
                } else { // match *
                    units.add(NAME_WILDCARD);
                }
                lastI = ++i;
            } else if (Character.isJavaIdentifierPart(c) || c == '.') {
                ++i;
            } else {
                throw new IllegalArgumentException("Invalid name pattern: " + pattern);
            }
        }
        if (lastI < i) { // match rest string
            units.add(new StringUnit(pattern.substring(lastI, i)));
        }
        return new NamePattern(units);
    }

    record NamePattern(List<NameUnit> units) {
        @Override
        public String toString() {
            return units.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(""));
        }
    }

    interface NameUnit {
    }

    static final NameUnit FULLNAME_WILDCARD = new NameUnit() {
        @Override
        public String toString() {
            return FULLNAME_WILDCARD_MARK;
        }
    };

    static final NameUnit NAME_WILDCARD = new NameUnit() {
        @Override
        public String toString() {
            return NAME_WILDCARD_MARK;
        }
    };

    record StringUnit(String content) implements NameUnit {
        @Override
        public String toString() {
            return content;
        }
    }

    /**
     * MethodPattern -> <ClassPattern: TypePattern NamePattern(ParamUnit...)>
     */
    static MethodPattern parseMethodPattern(String pattern) {
        try {
            List<String> splits = Arrays.stream(pattern.split("[<:\\s(,)>]"))
                    .filter(s -> !s.isEmpty())
                    .toList();
            ClassPattern klass = parseClassPattern(splits.get(0));
            TypePattern type = parseTypePattern(splits.get(1));
            NamePattern name = parseNamePattern(splits.get(2));
            List<ParamUnit> params = splits.stream()
                    .skip(3)
                    .map(Pattern::parseParamUnit)
                    .toList();
            return new MethodPattern(klass, type, name, params);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid method pattern: " + pattern);
        }
    }

    record MethodPattern(ClassPattern klass,
                         TypePattern retType, NamePattern name, List<ParamUnit> params) {
        @Override
        public String toString() {
            return "<" + klass + ": " + retType +
                    " " + name +
                    "(" + params.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(","))
                    + ")>";
        }
    }

    static ParamUnit parseParamUnit(String pattern) {
        return pattern.equals(PARAM_WILDCARD_MARK)
                ? PARAM_WILDCARD : parseTypePattern(pattern);
    }

    interface ParamUnit {
    }

    static final ParamUnit PARAM_WILDCARD = new ParamUnit() {
        @Override
        public String toString() {
            return PARAM_WILDCARD_MARK;
        }
    };

    static TypePattern parseTypePattern(String pattern) {
        boolean includeSubtypes;
        if (pattern.endsWith(SUB_MARK)) {
            includeSubtypes = true;
            pattern = pattern.substring(0, pattern.length() - 1);
        } else {
            includeSubtypes = false;
        }
        return new TypePattern(parseNamePattern(pattern), includeSubtypes);
    }

    static class TypePattern implements ParamUnit {

        private final NamePattern name;

        private final boolean includeSubtypes;

        TypePattern(NamePattern name, boolean includeSubtypes) {
            this.name = name;
            this.includeSubtypes = includeSubtypes;
        }

        NamePattern getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TypePattern other)) {
                return false;
            }
            return name.equals(other.name)
                    && includeSubtypes == other.includeSubtypes;
        }

        @Override
        public int hashCode() {
            return Hashes.hash(name, includeSubtypes);
        }

        @Override
        public String toString() {
            return includeSubtypes ? name + SUB_MARK : name.toString();
        }
    }

    /**
     * FieldPattern -> <ClassPattern: TypePattern NamePattern>
     */
    static FieldPattern parseFieldPattern(String pattern) {
        try {
            List<String> splits = Arrays.stream(pattern.split("[<:\\s>]"))
                    .filter(s -> !s.isEmpty())
                    .toList();
            ClassPattern klass = parseClassPattern(splits.get(0));
            TypePattern type = parseTypePattern(splits.get(1));
            NamePattern name = parseNamePattern(splits.get(2));
            return new FieldPattern(klass, type, name);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid field pattern: " + pattern);
        }
    }

    record FieldPattern(ClassPattern klass,
                        TypePattern type, NamePattern name) {
        @Override
        public String toString() {
            return "<" + klass + ": " + type + " " + name + ">";
        }
    }
}
