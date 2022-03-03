package pascal.taie.frontend.newfrontend.exposed;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.classes.JClassLoader;

public class WorldParaHolder {
    private static ClassHierarchy classHierarchy;
    private static JClassLoader classLoader;
    private static TypeSystem typeSystem;

    public static void setClassHierarchy(ClassHierarchy classHierarchy1) {
        classHierarchy = classHierarchy1;
    }

    public static ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }

    public static JClassLoader getClassLoader() {
        return classLoader;
    }

    public static void setClassLoader(JClassLoader classLoader) {
        WorldParaHolder.classLoader = classLoader;
    }

    public static TypeSystem getTypeSystem() {
        return typeSystem;
    }

    public static void setTypeSystem(TypeSystem typeSystem) {
        WorldParaHolder.typeSystem = typeSystem;
    }

    public static boolean isWorldReady() {
        return typeSystem != null && classHierarchy != null
                && classLoader != null;
    }

    public static void setWorld(ClassHierarchy hierarchy, TypeSystem typeSystem, JClassLoader loader) {
        setClassHierarchy(hierarchy);
        setClassLoader(loader);
        setTypeSystem(typeSystem);
    }
}
