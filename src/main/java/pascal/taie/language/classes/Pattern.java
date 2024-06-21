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
import java.util.List;

/**
 * Pattern parsing and representation.
 */
class Pattern {

    /**
     * ClassPattern -> NamePattern[^]
     */
    static ClassPattern ofC(String cp) {
        boolean includeSubclasses;
        if (cp.endsWith("^")) {
            includeSubclasses = true;
            cp = cp.substring(0, cp.length() - 1);
        } else {
            includeSubclasses = false;
        }
        return new ClassPattern(parseNamePattern(cp), includeSubclasses);
    }

    static class ClassPattern {

        private final NamePattern name;

        private final boolean includeSubclasses;

        ClassPattern(NamePattern name, boolean includeSubclasses) {
            this.name = name;
            this.includeSubclasses = includeSubclasses;
        }

        @Override
        public String toString() {
            return "ClassPattern{" +
                    ", name=" + name +
                    "includeSubclasses=" + includeSubclasses +
                    '}';
        }
    }

    static NamePattern parseNamePattern(String s) {
        List<NameUnit> units = new ArrayList<>();
        int i = 0, lastI = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '*') {
                if (lastI < i) { // match string
                    units.add(new StringUnit(s.substring(lastI, i)));
                }
                if (i + 1 < s.length() && s.charAt(i + 1) == '*') { // match **
                    units.add(STARSTAR);
                    ++i;
                } else { // match *
                    units.add(STAR);
                }
                lastI = ++i;
            } else if (Character.isJavaIdentifierPart(c) || c == '.') {
                ++i;
            } else {
                throw new IllegalArgumentException(s + " is an invalid NamePattern");
            }
        }
        if (lastI < i) { // match rest string
            units.add(new StringUnit(s.substring(lastI, i)));
        }
        return new NamePattern(units);
    }

    record NamePattern(List<NameUnit> units) {
    }

    interface NameUnit {
    }

    static final NameUnit STARSTAR = new NameUnit() {
        @Override
        public String toString() {
            return "STARSTAR";
        }
    };

    static final NameUnit STAR = new NameUnit() {
        @Override
        public String toString() {
            return "STAR";
        }
    };

    record StringUnit(String content) implements NameUnit {
    }

    /**
     * MethodPattern -> <ClassPattern: TypePattern NamePattern(ParamUnit...)>
     */
    static MethodPattern ofM(String mp) {
        throw new UnsupportedOperationException();
    }

    record MethodPattern(ClassPattern klass,
                         TypePattern retType, NamePattern name, List<ParamUnit> params) {
    }

    interface ParamUnit {
    }

    static final ParamUnit WILDCARD = new ParamUnit() {
        @Override
        public String toString() {
            return "WILDCARD";
        }
    };

    static TypePattern parseTypePattern(String tp) {
        boolean includeSubtypes;
        if (tp.endsWith("^")) {
            includeSubtypes = true;
            tp = tp.substring(0, tp.length() - 1);
        } else {
            includeSubtypes = false;
        }
        return new TypePattern(parseNamePattern(tp), includeSubtypes);
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
        public String toString() {
            return "TypePattern{" +
                    "name=" + name +
                    ", includeSubtypes=" + includeSubtypes +
                    '}';
        }
    }

    /**
     * FieldPattern -> <ClassPattern: TypePattern NamePattern>
     */
    static FieldPattern ofF(String fp) {
        try {
            String[] splits = fp.split("[<:\\s>]");
            ClassPattern klass = ofC(splits[0]);
            TypePattern type = parseTypePattern(splits[1]);
            NamePattern name = parseNamePattern(splits[2]);
            return new FieldPattern(klass, type, name);
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal field pattern: " + fp);
        }
    }

    record FieldPattern(ClassPattern klass,
                        TypePattern type, NamePattern name) {
    }
}
