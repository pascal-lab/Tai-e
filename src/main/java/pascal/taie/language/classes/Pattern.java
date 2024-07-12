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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.compile;

/**
 * Pattern representation and parsing for class/method/field signatures.
 */
class Pattern {

    static final String SUB_MARK = "^";

    static final String NAME_WILDCARD_MARK = "*";

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

    record ClassPattern(NamePattern name, boolean includeSubclasses) {

        boolean isExactMatch() {
            return !name.hasWildcard() && !includeSubclasses;
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
                units.add(NAME_WILDCARD); // match wildcard (*)
                lastI = ++i;
            } else if (Character.isJavaIdentifierPart(c)
                    || c == '.' // package separator
                    || c == '[' || c == ']' // array type
                    || c == '<' || c == '>' // <init> and <clinit>
            ) { // character c is valid
                ++i;
            } else {
                throw new IllegalArgumentException("Invalid name pattern: " + pattern);
            }
        }
        if (lastI < i) { // match last string
            units.add(new StringUnit(pattern.substring(lastI, i)));
        }
        return new NamePattern(units);
    }

    record NamePattern(List<NameUnit> units) implements Iterable<NameUnit> {
        boolean hasWildcard() {
            return units.stream().anyMatch(NAME_WILDCARD::equals);
        }

        @Nonnull
        @Override
        public Iterator<NameUnit> iterator() {
            return units.iterator();
        }

        @Override
        public String toString() {
            return units.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(""));
        }
    }

    interface NameUnit {
    }

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
     * MethodPattern -> &lt;ClassPattern: TypePattern NamePattern(ParamUnit...)&gt;
     */
    static MethodPattern parseMethodPattern(String pattern) {
        try {
            // method names of constructors (<init>) and static initializers
            // (<clinit>) contain '<' and '>', so we cannot simply treat them
            // as separator like in field pattern
            List<String> splits = Arrays.stream(pattern
                            .substring(1, pattern.length() - 1)
                            .split("[:\\s(,)]"))
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
            throw new IllegalArgumentException("Invalid method pattern: " + pattern, e);
        }
    }

    record MethodPattern(ClassPattern klass,
                         TypePattern retType, NamePattern name, List<ParamUnit> params) {
        boolean isExactMatch() {
            return klass.isExactMatch()
                    && retType.isExactMatch()
                    && !name.hasWildcard()
                    && params.stream().allMatch(ParamUnit::isExactMatch);
        }

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
        int i = pattern.indexOf('{'); // check if pattern ends with {...}
        if (i == -1) {
            return new ParamUnit(parseTypePattern(pattern), Repeat.ONCE);
        } else {
            TypePattern type = parseTypePattern(pattern.substring(0, i));
            Repeat repeat = Repeat.parse(pattern.substring(i));
            return new ParamUnit(type, repeat);
        }
    }

    record ParamUnit(TypePattern type, Repeat repeat) {
        boolean isExactMatch() {
            return type.isExactMatch() && repeat.equals(Repeat.ONCE);
        }

        @Override
        public String toString() {
            String typeStr = type.toString();
            return repeat.equals(Repeat.ONCE) ? typeStr : typeStr + repeat;
        }
    }

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

    record TypePattern(NamePattern name, boolean includeSubtypes) {

        boolean isExactMatch() {
            return !name.hasWildcard() && !includeSubtypes;
        }

        @Override
        public String toString() {
            return includeSubtypes ? name + SUB_MARK : name.toString();
        }
    }

    record Repeat(int min, int max) {

        static final Repeat ONCE = new Repeat(1, 1);

        /**
         * Upper bound of number of method parameters.
         */
        static final int MAX = 256;

        // {N}
        private static final java.util.regex.Pattern N = compile("\\{(\\d+)}");

        // {N+}
        private static final java.util.regex.Pattern N_OR_MORE = compile("\\{(\\d+)\\+}");

        // {N-M}
        private static final java.util.regex.Pattern RANGE = compile("\\{(\\d+)-(\\d+)}");

        private static Repeat parse(String str) {
            Matcher nOrMore = N_OR_MORE.matcher(str);
            if (nOrMore.matches()) {
                int min = Integer.parseInt(nOrMore.group(1));
                return new Repeat(min, MAX);
            }
            Matcher n = N.matcher(str);
            if (n.matches()) {
                int times = Integer.parseInt(n.group(1));
                return new Repeat(times, times);
            }
            Matcher range = RANGE.matcher(str);
            if (range.matches()) {
                int min = Integer.parseInt(range.group(1));
                int max = Integer.parseInt(range.group(2));
                return new Repeat(min, max);
            }
            throw new IllegalArgumentException("Invalid parameter repetition: " + str);
        }

        @Override
        public String toString() {
            if (min == max) {
                return "{" + min + "}";
            } else if (max == MAX) {
                return "{" + min + "+}";
            } else {
                return "{" + min + "-" + max + "}";
            }
        }
    }

    /**
     * FieldPattern -> &lt;ClassPattern: TypePattern NamePattern&gt;
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
            throw new IllegalArgumentException("Invalid field pattern: " + pattern, e);
        }
    }

    record FieldPattern(ClassPattern klass,
                        TypePattern type, NamePattern name) {
        boolean isExactMatch() {
            return klass.isExactMatch()
                    && type.isExactMatch()
                    && !name.hasWildcard();
        }

        @Override
        public String toString() {
            return "<" + klass + ": " + type + " " + name + ">";
        }
    }
}
