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

package pascal.taie.language.type;

public enum PrimitiveType implements Type {

    INT("int"),
    CHAR("char"),
    BOOLEAN("boolean"),
    BYTE("byte"),
    LONG("long"),
    FLOAT("float"),
    DOUBLE("double"),
    SHORT("short");

    /**
     * Name of this type.
     */
    private final String name;

    PrimitiveType(String name) {
        this.name = name;
    }

    /**
     * @return true if given name represents a primitive type, otherwise false.
     */
    public static boolean isPrimitiveType(String name) {
        for (PrimitiveType t : values()) {
            if (t.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the primitive type specified by specific name.
     * @throws IllegalArgumentException if given name is irrelevant to any primitive type.
     */
    public static PrimitiveType get(String name) {
        for (PrimitiveType t : values()) {
            if (t.name.equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException(name + " is not primitive type");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * JVM Spec. (2.11.1): most operations on values of actual types
     * boolean, byte, char, and short are correctly performed by instructions
     * operating on values of computational type int.
     *
     * @return {@code true} if the values of this type are represented
     * as integers in computation.
     */
    public boolean asInt() {
        return switch (this) {
            case INT, CHAR, BOOLEAN, BYTE, SHORT -> true;
            default -> false;
        };
    }
}
