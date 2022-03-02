package pascal.taie.frontend.newfrontend.exposed;

import pascal.taie.language.classes.ClassHierarchy;

public class ClassHierarchyHolder {
    private static ClassHierarchy classHierarchy;

    public static void setClassHierarchy(ClassHierarchy classHierarchy1) {
        classHierarchy = classHierarchy1;
    }

    public static ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }
}
