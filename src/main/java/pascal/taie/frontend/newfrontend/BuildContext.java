package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import pascal.taie.World;
import pascal.taie.frontend.newfrontend.asyncir.IRService;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

public class BuildContext {

    private final JClassLoader defaultClassLoader;

    private final TypeSystem typeSystem;

    private ClassHierarchy hierarchy;

    final ConcurrentMap<JMethod, JSRInlinerAdapter> method2Source;

    final IRService irService = new IRService();

    private BuildContext(JClassLoader defaultClassLoader, TypeSystem typeSystem) {
        this.defaultClassLoader = defaultClassLoader;
        this.typeSystem = typeSystem;
        method2Source = Maps.newConcurrentMap();
    }

    static BuildContext buildContext;

    static {
        World.registerResetCallback(() -> {
            buildContext = null;
        });
    }

    public static BuildContext get() {
        assert buildContext != null;
        return buildContext;
    }

    static void make(JClassLoader loader) {
        assert buildContext == null;
        buildContext = new BuildContext(loader, new TempTypeSystem(loader));
    }

    public void setHierarchy(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    public JClass getClassByName(String name) {
        JClass klass = defaultClassLoader.loadClass(name);
        assert klass != null;
        return klass;
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
        int sort = t.getSort();
        if (sort == org.objectweb.asm.Type.VOID) {
            return VoidType.VOID;
        } else if (sort < org.objectweb.asm.Type.ARRAY) {
            return switch (sort) {
                case org.objectweb.asm.Type.BOOLEAN -> PrimitiveType.BOOLEAN;
                case org.objectweb.asm.Type.BYTE -> PrimitiveType.BYTE;
                case org.objectweb.asm.Type.CHAR -> PrimitiveType.CHAR;
                case org.objectweb.asm.Type.SHORT -> PrimitiveType.SHORT;
                case org.objectweb.asm.Type.INT -> PrimitiveType.INT;
                case org.objectweb.asm.Type.LONG -> PrimitiveType.LONG;
                case org.objectweb.asm.Type.FLOAT -> PrimitiveType.FLOAT;
                case org.objectweb.asm.Type.DOUBLE -> PrimitiveType.DOUBLE;
                default -> throw new UnsupportedOperationException();
            };
        } else if (sort == org.objectweb.asm.Type.ARRAY) {
            return typeSystem.getArrayType(fromAsmType(t.getElementType()), t.getDimensions());
        } else if (sort == org.objectweb.asm.Type.OBJECT) {
            return typeSystem.getClassType(t.getClassName());
        } else {
            // t maybe a function ? error
            throw new IllegalArgumentException();
        }
    }

    private final Map<String, Pair<List<Type>, Type>> methodDescriptorCache = Maps.newConcurrentMap();

    public Pair<List<Type>, Type> fromAsmMethodType(String descriptor) {
        // normally we want to avoid using caching
        // but this method will be called very frequently
        // caching is able to save ~70% of calculation time
        return methodDescriptorCache.computeIfAbsent(descriptor, this::internalFromAsmMethodType);
    }

    public Pair<List<Type>, Type> internalFromAsmMethodType(String descriptor) {
        org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(descriptor);
        return fromAsmMethodType(t);
    }

    public Pair<List<Type>, Type> fromAsmMethodType(org.objectweb.asm.Type t) {
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

    public MethodType toMethodType(org.objectweb.asm.Type t) {
        Pair<List<Type>, Type> temp = fromAsmMethodType(t);
        return MethodType.get(temp.first(), temp.second());
    }

    public JClass toJClass(String internalName) {
        if (internalName.startsWith("[")) {
            return Utils.getObject().getJClass();
        } else {
            return getClassByName(org.objectweb.asm.Type.getObjectType(internalName).getClassName());
        }
    }

    public void noticeClassSource(JClass clazz, AsmSource source) {
        irService.putClassSource(clazz, source);
    }

    public IRService getIRService() {
        return irService;
    }

    public ClassHierarchy getClassHierarchy() {
        return hierarchy;
    }

}
