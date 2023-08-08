package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
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
import java.util.concurrent.ConcurrentMap;

public class BuildContext {

    private final JClassLoader defaultClassLoader;

    private final TypeSystem typeSystem;

    private ClassHierarchy hierarchy;

    final ConcurrentMap<JMethod, JSRInlinerAdapter> method2Source;

    final ConcurrentMap<JClass, AsmSource> jclass2Node;

    private BuildContext(JClassLoader defaultClassLoader, TypeSystem typeSystem) {
        this.defaultClassLoader = defaultClassLoader;
        this.typeSystem = typeSystem;
        jclass2Node = Maps.newConcurrentMap();
        method2Source = Maps.newConcurrentMap();
    }

    static BuildContext buildContext;

    static BuildContext get() {
        if (buildContext == null) {
            throw new IllegalStateException();
        }
        return buildContext;
    }

    static void make(JClassLoader loader) {
        buildContext = new BuildContext(loader, new TempTypeSystem(loader));
    }

    public void setHierarchy(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    public JClass getClassByName(String name) {
        return defaultClassLoader.loadClass(name);
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
        org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(descriptor);
        return fromAsmMethodType(t);
    }

    public Pair<List<Type>, Type> fromAsmMethodType(org.objectweb.asm.Type t) {
        // TODO: need memorize ?
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
        ReferenceType type = fromAsmInternalName(internalName);
        if (type instanceof ArrayType) {
            return typeSystem.getClassType(ClassNames.ARRAY).getJClass();
        } else if (type instanceof ClassType t) {
            return t.getJClass();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public ClassHierarchy getClassHierarchy() {
        return hierarchy;
    }

    public AsmMethodSource getSource(JMethod method) {
        if (method2Source.get(method) != null) {
            return new AsmMethodSource(method2Source.get(method),
                    jclass2Node.get(method.getDeclaringClass()).getClassFileVersion());
        }

        AsmSource source = jclass2Node.get(method.getDeclaringClass());
        BuildContext ctx = this;

        JClass c = method.getDeclaringClass();
        synchronized (c) {
            if (method2Source.get(method) == null) {
                source.r().accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        JSRInlinerAdapter adapter = new JSRInlinerAdapter(null, access, name, descriptor, signature, exceptions);
                        org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(descriptor);
                        var paramTypes = Arrays.stream(t.getArgumentTypes())
                                .map(ctx::fromAsmType)
                                .toList();
                        var retType = BuildContext.get().fromAsmType(t.getReturnType());
                        JMethod method1 = method.getDeclaringClass().getDeclaredMethod(Subsignature.get(name, paramTypes, retType));
                        method2Source.put(method1, adapter);
                        return adapter;
                    }
                }, ClassReader.EXPAND_FRAMES);
            }

            assert method2Source.get(method) != null;
            return new AsmMethodSource(method2Source.get(method),
                    jclass2Node.get(method.getDeclaringClass()).getClassFileVersion());
        }
    }
}
