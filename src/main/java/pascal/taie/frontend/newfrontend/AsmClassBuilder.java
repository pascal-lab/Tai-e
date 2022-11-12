package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static pascal.taie.frontend.newfrontend.Utils.fromAsmModifier;
import static pascal.taie.frontend.newfrontend.Utils.getBinaryName;

public class AsmClassBuilder implements JClassBuilder {

    private final AsmSource source;

    private final JClass jClass;

    private Set<Modifier> modifiers;

    private JClass superClass;

    private List<JClass> interfaces;

    private JClass outerClass;

    private final List<JField> fields;

    private final List<JMethod> methods;

    public AsmClassBuilder(
            AsmSource source, JClass jClass) {
        this.source = source;
        this.jClass = jClass;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }

    @Override
    public void build(JClass jclass) {
        buildAll();
        if (jclass != this.jClass) {
            throw new IllegalArgumentException();
        }
        jclass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String getSimpleName() {
        return getSimpleName(source.getClassName());
    }

    @Override
    public ClassType getClassType() {
        return BuildContext.get()
                .getTypeSystem()
                .getClassType(source.getClassName());
    }

    @Override
    public JClass getSuperClass() {
        return superClass;
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return interfaces;
    }

    @Override
    public JClass getOuterClass() {
        return outerClass;
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return fields;
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        return methods;
    }

    @Override
    public AnnotationHolder getAnnotationHolder() {
        return null;
    }

    @Override
    public boolean isApplication() {
        return false;
    }

    @Override
    public boolean isPhantom() {
        // TODO: handle phantom class
        return false;
    }

    private void buildAll() {
        CVisitor visitor = new CVisitor();
        source.r().accept(visitor, ClassReader.SKIP_FRAMES);
    }

    private String getSimpleName(String binaryName) {
        return binaryName.substring(binaryName.indexOf("."));
    }

    private static JClass getClassByName(String internalName) {
        return BuildContext.get()
                .getClassMap()
                .get(getBinaryName(internalName));
    }

    class CVisitor extends ClassVisitor {

        public CVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
            if (superName != null) {
                superClass = getClassByName(superName);
            }

            AsmClassBuilder.this.interfaces = Arrays.stream(interfaces)
                    .map(AsmClassBuilder::getClassByName)
                    .toList();

            modifiers = fromAsmModifier(access);
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            AsmClassBuilder.this.outerClass = getClassByName(owner);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            // TODO: check what attribute is needed.
        }

        @Override
        public FieldVisitor visitField(
                int access,
                String name,
                String descriptor,
                String signature,
                Object value) {
            Type type = BuildContext.get().fromAsmType(descriptor);
            fields.add(new JField(jClass, name,
                    fromAsmModifier(access), type, null));
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(descriptor);
            return new MVisitor(fromAsmModifier(access),
                    name,
                    Arrays.stream(exceptions)
                            .map(BuildContext.get()::classTypeFromAsmType)
                            .toList(),
                    Arrays.stream(t.getArgumentTypes())
                            .map(BuildContext.get()::fromAsmType)
                            .toList(),
                    BuildContext.get().fromAsmType(t.getReturnType()));
        }

    }

    class MVisitor extends MethodVisitor {

        private final Set<Modifier> modifiers;

        private final String methodName;

        private final List<ClassType> exceptions;

        private final List<Type> paramTypes;

        private final Type retType;

        @Nullable
        private List<String> paramName;

        public MVisitor(Set<Modifier> modifiers, String methodName,
                        List<ClassType> exceptions, List<Type> paramTypes, Type retType) {
            super(Opcodes.ASM9);
            this.modifiers = modifiers;
            this.methodName = methodName;
            this.exceptions = exceptions;
            this.paramTypes = paramTypes;
            this.retType = retType;
        }

        @Override
        public void visitParameter(String name, int access) {
            if (paramName == null) {
                paramName = new ArrayList<>();
            }
            paramName.add(name);
        }

        @Override
        public void visitEnd() {
           AsmClassBuilder.this.methods.add(
                   new JMethod(jClass, methodName, modifiers, paramTypes,
                           retType, exceptions,
                           null, null,
                           paramName, null)
           );
        }

    }
}
