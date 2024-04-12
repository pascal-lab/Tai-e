package pascal.taie.frontend.newfrontend.report;

import pascal.taie.language.classes.JClass;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ClassHierarchyTree {
    private final Set<JClass> classes;

    public ClassHierarchyTree(List<JClass> classes) {
        this.classes = new HashSet<>(classes);
    }

    private Set<JClass> hasDrawn;

    public String toDotFile() {
        hasDrawn = new HashSet<>();
        Set<JClass> hasDrawnEdge = new HashSet<>();
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
