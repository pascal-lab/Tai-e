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

package pascal.taie.frontend.newfrontend.closedworld;

import java.util.Collection;

public class InternalNameVisitor {
    public static void visitDescriptor(String descriptor, Collection<String> container) {
        char first = descriptor.charAt(0);
        if (first == 'L') {
            String internalName = descriptor.substring(1, descriptor.length() - 1);
            container.add(internalName);
        } else if (first == '[') {
            extractArrayType(descriptor, container);
        } else if (first == '(') {
            int i = 1;
            while (descriptor.charAt(i) != ')') {
                char now = descriptor.charAt(i);
                switch (now) {
                    case 'B', 'C', 'D', 'I', 'F', 'J', 'S', 'Z' -> {
                        i++;
                    }
                    case '[' -> {
                        while (descriptor.charAt(i) == '[') {
                            i++;
                        }
                        if (descriptor.charAt(i) == 'L') {
                            i = readNext(descriptor, container, i);
                        } else {
                            i++;
                        }
                    }
                    case 'L' -> {
                        i = readNext(descriptor, container, i);
                    }
                    default -> throw new UnsupportedOperationException();
                }
            }
            i++;
            if (descriptor.charAt(i) == 'L') {
                String internalName = descriptor.substring(i + 1, descriptor.length() - 1);
                container.add(internalName);
            }
        }
    }

    public static void visitInternalName(String internalName, Collection<String> containers) {
        if (internalName.charAt(0) == '[') {
            extractArrayType(internalName, containers);
        } else {
            containers.add(internalName);
        }
    }

    private static void extractArrayType(String internalName, Collection<String> containers) {
        int i = 0;
        while (internalName.charAt(i) == '[') {
            i++;
        }
        if (internalName.charAt(i) == 'L') {
            String res = internalName.substring(i + 1, internalName.length() - 1);
            containers.add(res);
        }
    }

    private static int readNext(String descriptor, Collection<String> container, int i) {
        i++;
        int start = i;
        while (descriptor.charAt(i) != ';') {
            i++;
        }
        String internalName = descriptor.substring(start, i);
        container.add(internalName);
        i++;
        return i;
    }
}
