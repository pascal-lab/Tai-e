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

import java.util.List;

/**
 * Pattern parsing and representation.
 */
class Pattern {

    static ClassPattern ofC(String cp) {
        throw new UnsupportedOperationException();
    }

    static MethodPattern ofM(String mp) {
        throw new UnsupportedOperationException();
    }

    static FieldPattern ofF(String fp) {
        throw new UnsupportedOperationException();
    }

    interface NameUnit {
    }

    record StringUnit(String content) implements NameUnit {
    }

    static final NameUnit STAR = new NameUnit() {
        @Override
        public String toString() {
            return "STAR";
        }
    };

    static final NameUnit STARSTAR = new NameUnit() {
        @Override
        public String toString() {
            return "STARSTAR";
        }
    };

    record NamePattern(List<NameUnit> units) {
    }

    static class TypePattern implements ParamUnit {

        private final NamePattern name;

        TypePattern(NamePattern name) {
            this.name = name;
        }

        public NamePattern getName() {
            return name;
        }
    }

    static class ClassPattern extends TypePattern {

        private final boolean includeSubclasses;

        ClassPattern(NamePattern name, boolean includeSubclasses) {
            super(name);
            this.includeSubclasses = includeSubclasses;
        }

        @Override
        public String toString() {
            return "ClassPattern{" +
                    ", name=" + getName() +
                    "includeSubclasses=" + includeSubclasses +
                    '}';
        }
    }

    interface ParamUnit {
    }

    static final ParamUnit WILDCARD = new ParamUnit() {
        @Override
        public String toString() {
            return "WILDCARD";
        }
    };

    record MethodPattern(ClassPattern klass,
                         TypePattern retType, NamePattern name, List<ParamUnit> params) {
    }

    record FieldPattern(ClassPattern klass,
                        TypePattern type, NamePattern name) {
    }
}
