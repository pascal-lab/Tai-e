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

package pascal.taie.language.generics;

/**
 * In <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-BaseType">
 * JVM Spec. 4.3.2 Base Types</a>,
 * a <i>base type</i> is one of the following primitive types:
 * <ul>
 *     <li>byte</li>
 *     <li>char</li>
 *     <li>double</li>
 *     <li>float</li>
 *     <li>int</li>
 *     <li>long</li>
 *     <li>short</li>
 *     <li>boolean</li>
 * </ul>
 */
public enum BaseType implements TypeGSignature {

    BYTE('B', "byte"),
    CHAR('C', "char"),
    DOUBLE('D', "double"),
    FLOAT('F', "float"),
    INT('I', "int"),
    LONG('J', "long"),
    SHORT('S', "short"),
    BOOLEAN('Z', "boolean");

    /**
     * Descriptor of this type.
     */
    private final char descriptor;

    /**
     * Name of this type.
     */
    private final String name;

    BaseType(char descriptor, String name) {
        this.descriptor = descriptor;
        this.name = name;
    }

    /**
     * @return the primitive type specified by specific name.
     * @throws IllegalArgumentException if given name is irrelevant to any primitive type.
     */
    public static BaseType of(char descriptor) {
        for (BaseType t : values()) {
            if (t.descriptor == descriptor) {
                return t;
            }
        }
        throw new IllegalArgumentException(descriptor + " is not base type");
    }

    @Override
    public String toString() {
        return name;
    }

}
