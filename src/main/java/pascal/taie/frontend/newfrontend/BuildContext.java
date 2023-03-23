package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildContext {

    private final Map<String, JClass> classMap;

    private final TypeSystem typeSystem;

    private BuildContext(Map<String, JClass> classMap, TypeSystem typeSystem) {
        this.classMap = classMap;
        this.typeSystem = typeSystem;
    }

    static BuildContext buildContext;

    static BuildContext get() {
        if (buildContext == null) {
            throw new IllegalStateException();
        }
        return buildContext;
    }

    static void make(Map<String, JClass> classMap, JClassLoader loader) {
        buildContext = new BuildContext(classMap, new TempTypeSystem(loader));
    }

    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    public Map<String, JClass> getClassMap() {
        return classMap;
    }

    public ReferenceType fromAsmInternalName(String internalName) {
        return (ReferenceType) fromAsmType(
                org.objectweb.asm.Type.getObjectType(internalName));
    }

    public Type fromAsmType(String descriptor) {
        org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(descriptor);
        return fromAsmType(t);
    }

    public Type fromAsmType(org.objectweb.asm.Type t) {
        if (t.getSort() == org.objectweb.asm.Type.VOID) {
            return VoidType.VOID;
        } else if (t.getSort() < org.objectweb.asm.Type.ARRAY) {
            // t is a primitive type
            return PrimitiveType.get(StringReps.toTaieTypeDesc(t.getDescriptor()));
        } else if (t.getSort() == org.objectweb.asm.Type.ARRAY) {
            return typeSystem.getArrayType(fromAsmType(t.getElementType()), t.getDimensions());
        } else if(t.getSort() == org.objectweb.asm.Type.OBJECT) {
            return typeSystem.getType(t.getClassName());
        } else {
            // t maybe a function ? error
            throw new IllegalArgumentException();
        }
    }

    public Pair<List<Type>, Type> fromAsmMethodType(String descriptor) {
        // TODO: need memorize ?
        org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(descriptor);
        if (t.getSort() == org.objectweb.asm.Type.METHOD) {
            List<Type> paramTypes = new ArrayList<>();
            for (org.objectweb.asm.Type t1 : t.getArgumentTypes()) {
                paramTypes.add(fromAsmType(t1));
            }
            return new Pair<>(paramTypes, fromAsmType(t.getReturnType()));
        } else {
            throw new IllegalArgumentException();
        }
    }
}
