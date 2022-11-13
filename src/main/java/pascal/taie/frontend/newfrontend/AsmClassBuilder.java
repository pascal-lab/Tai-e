package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.AnnotationElement;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.EnumElement;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static pascal.taie.frontend.newfrontend.Utils.*;

public class AsmClassBuilder implements JClassBuilder {

    private final AsmSource source;

    private final JClass jClass;

    private Set<Modifier> modifiers;

    private JClass superClass;

    private List<JClass> interfaces;

    private JClass outerClass;

    private final List<JField> fields;

    private final List<JMethod> methods;

    /**
     * annotations for this class
     */
    private final List<Annotation> annotations;

    public AsmClassBuilder(
            AsmSource source, JClass jClass) {
        this.source = source;
        this.jClass = jClass;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.annotations = new ArrayList<>();
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
        return AnnotationHolder.make(annotations);
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
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return new AnnoVisitor(descriptor, annotations::add);
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
            return new FVisitor(annotations -> fields.add(new JField(jClass, name,
                fromAsmModifier(access), type, AnnotationHolder.make(annotations))));
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

    class FVisitor extends FieldVisitor {
        private final List<Annotation> annotations;

        private final Consumer<List<Annotation>> consumer;

        protected FVisitor(Consumer<List<Annotation>> consumer) {
            super(Opcodes.ASM9);
            this.consumer = consumer;
            this.annotations = new ArrayList<>();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return new AnnoVisitor(descriptor, annotations::add);
        }

        @Override
        public void visitEnd() {
            consumer.accept(annotations);
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
            // TODO: handle method annotation
           AsmClassBuilder.this.methods.add(
                   new JMethod(jClass, methodName, modifiers, paramTypes,
                           retType, exceptions,
                           null, null,
                           paramName, null)
           );
        }
    }

    /**
     * Annotation visitor to build annotations
     */
    class AnnoVisitor extends AnnotationVisitor {

        private final String type;

        private final Map<String, Element> pairs;

        private final Consumer<Annotation> consumer;

        public AnnoVisitor(String descriptor, Consumer<Annotation> consumer) {
            super(Opcodes.ASM9);
            this.type = descriptor;
            this.pairs = Maps.newHybridMap();
            this.consumer = consumer;
        }

        /**
         * primitive array will use this method
         */
        @Override
        public void visit(String name, Object value) {
            pairs.put(name, toElement(value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            // TODO: check string rep here
            pairs.put(name, new EnumElement(StringReps.toTaieTypeDesc(descriptor), value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return new AnnoVisitor(descriptor,
                    i -> pairs.put(name, new AnnotationElement(i)));
        }

        /**
         * Non-primitive array will use this method
         */
        @Override
        public AnnotationVisitor visitArray(String name) {
            return new AnnoArrayVisitor(i ->
                    pairs.put(name, new ArrayElement(i)));
        }

        @Override
        public void visitEnd() {
            consumer.accept(new Annotation(type, pairs));
            super.visitEnd();
        }
    }

    class AnnoArrayVisitor extends AnnotationVisitor {
        private final List<Element> collector;

        private final Consumer<List<Element>> consumer;

        public AnnoArrayVisitor(Consumer<List<Element>> consumer) {
            super(Opcodes.ASM9);
            this.collector = new ArrayList<>();
            this.consumer = consumer;
        }


        @Override
        public void visit(String name, Object value) {
            collector.add(toElement(value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            collector.add(new EnumElement(StringReps.toTaieTypeDesc(descriptor), value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return new AnnoVisitor(descriptor, this::add);
        }

        @Override
        public void visitEnd() {
            consumer.accept(collector);
        }

        private void add(Annotation a) {
            collector.add(new AnnotationElement(a));
        }
    }
}
