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

package pascal.taie.frontend.newfrontend.report;

import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Sets;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ClassHierarchyTree {
    private final Set<JClass> classes;

    public ClassHierarchyTree(List<JClass> classes) {
        this.classes = Sets.newSet();
    }

    private Set<JClass> hasDrawn;

    public String toDotFile() {
        hasDrawn = Sets.newSet();
        Set<JClass> hasDrawnEdge = Sets.newSet();
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("rankdir=BT;\n");
        for (JClass jClass : classes) {
            sb.append(drawClass(jClass, true));
            hasDrawn.add(jClass);
        }
        Queue<JClass> workList = new LinkedList<>(classes);
        while (!workList.isEmpty()) {
            JClass jClass = workList.poll();
            if (hasDrawnEdge.contains(jClass)) {
                continue;
            }
            hasDrawnEdge.add(jClass);
            if (jClass.getSuperClass() != null) {
                drawEdge(sb, jClass, jClass.getSuperClass(), false);
                workList.add(jClass.getSuperClass());
            }
            for (JClass interfaces : jClass.getInterfaces()) {
                drawEdge(sb, jClass, interfaces, true);
                workList.add(interfaces);
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private void drawEdge(StringBuilder sb, JClass from, JClass to, boolean isInterface) {
        for (JClass klass : List.of(from, to)) {
            if (!hasDrawn.contains(klass)) {
                sb.append(drawClass(klass, false));
                hasDrawn.add(klass);
            }
        }
        if (isInterface) {
            sb.append(drawInterfaceEdge(from, to));
        } else {
            sb.append(drawSuperEdge(from, to));
        }
    }

    private static String showName(JClass klass) {
        return "\"" + klass.getName() + "\"";
    }

    public static String drawClass(JClass klass, boolean inTargets) {
        // use red for interfaces
        // use box for targets
        return showName(klass) + " [color=" + (klass.isInterface() ? "red" : "black") +
                ", shape=" + (inTargets ? "box" : "ellipse") + "];\n";
    }

    public static String drawSuperEdge(JClass subClass, JClass superClass) {
        return showName(subClass) + " -> " + showName(superClass) + ";\n";
    }

    public static String drawInterfaceEdge(JClass subClass, JClass interfaceClass) {
        // use dotted lines
        return showName(subClass) + " -> " + showName(interfaceClass) + " [style=dotted];\n";
    }
}
