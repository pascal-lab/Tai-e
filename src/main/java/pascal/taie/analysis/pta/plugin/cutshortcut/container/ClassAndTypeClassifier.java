package pascal.taie.analysis.pta.plugin.cutshortcut.container;

import pascal.taie.World;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ContainerType;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

public class ClassAndTypeClassifier {
    private static final ClassHierarchy hierarchy = World.get().getClassHierarchy();
    private static final TypeSystem typeSystem = World.get().getTypeSystem();

    private static final JClass mapClass = hierarchy.getClass("java.util.Map");
    private static final JClass mapEntryClass = hierarchy.getClass("java.util.Map$Entry");
    private static final JClass collectionClass = hierarchy.getClass("java.util.Collection");
    private static final JClass iteratorClass = hierarchy.getClass("java.util.Iterator");

    private static final Type hashtableType = typeSystem.getType("java.util.Hashtable");
    private static final Type vectorType = typeSystem.getType("java.util.Vector");

    public static ContainerType ClassificationOf(Type type) {
        JClass clz = hierarchy.getClass(type.getName());
        return ClassificationOf(clz);
    }

    public static ContainerType ClassificationOf(JClass clz) {
        if (clz == null)
            return ContainerType.OTHER;
        else if (hierarchy.isSubclass(mapClass, clz))
            return ContainerType.MAP;
        else if (hierarchy.isSubclass(collectionClass, clz))
            return ContainerType.COLLECTION;
        else if (hierarchy.isSubclass(iteratorClass, clz))
            return ContainerType.ITER;
        return ContainerType.OTHER;
    }

    public static JClass getOuterClass(JClass inner) {
        if (inner != null && inner.hasOuterClass())
            inner = inner.getOuterClass();
        return inner;
    }

    public static boolean isVectorType(Type vector) {
        return typeSystem.isSubtype(vectorType, vector);
    }

    public static boolean isHashtableType(Type hashtable) {
        return typeSystem.isSubtype(hashtableType, hashtable);
    }

    public static boolean isMapEntryClass(JClass entry) {
        return hierarchy.isSubclass(mapEntryClass, entry);
    }
}
