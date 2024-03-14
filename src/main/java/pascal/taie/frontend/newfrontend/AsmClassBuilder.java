package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
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
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final int version;

    public AsmClassBuilder(
            AsmSource source, JClass jClass) {
        this.source = source;
        this.jClass = jClass;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.version = source.getClassFileVersion();
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
        return source.isApplication();
    }

    @Override
    public boolean isPhantom() {
        return false;
    }

    @Nullable
    @Override
    public ClassGSignature getGSignature() {
        // TODO: implement this
        return null;
    }

    private void buildAll() {
        if (source.node() == null) {
            CVisitor visitor = new CVisitor();
            source.r().accept(visitor, ClassReader.SKIP_CODE);
        } else {
            ClassNode node = source.node();
            if (node.superName != null) {
                this.superClass = getClassByName(node.superName);
            }
            this.interfaces = node.interfaces.stream()
                    .map(AsmClassBuilder::getClassByName)
                    .toList();
            if (node.outerClass != null) {
                this.outerClass = getClassByName(node.outerClass);
            }
            this.modifiers = fromAsmModifier(node.access);
            for (FieldNode fieldNode : node.fields) {
                fields.add(new JField(jClass, fieldNode.name,
                        fromAsmModifier(fieldNode.access),
                        BuildContext.get().fromAsmType(fieldNode.desc),
                        null,
                        AnnotationHolder.make(new FVisitor(annotations -> {
                        }).annotations)));
            }
            if (node.visibleAnnotations != null) {
                for (AnnotationNode annotationNode : node.visibleAnnotations) {
                    annotationNode.accept(new AnnoVisitor(annotationNode.desc, annotations::add));
                }
            }
            for (MethodNode methodNode : node.methods) {
                assert methodNode instanceof JSRInlinerAdapter;
                Set<Modifier> modifiers1 = fromAsmModifier(methodNode.access);
                List<Type> paramTypes = new ArrayList<>();
                org.objectweb.asm.Type methodType = org.objectweb.asm.Type.getMethodType(methodNode.desc);
                for (org.objectweb.asm.Type t1 : methodType.getArgumentTypes()) {
                    paramTypes.add(BuildContext.get().fromAsmType(t1));
                }
                Type retType = BuildContext.get().fromAsmType(methodType.getReturnType());
                List<ClassType> exceptions = new ArrayList<>();
                if (methodNode.exceptions != null) {
                    for (String exception : methodNode.exceptions) {
                        exceptions.add((ClassType) BuildContext.get().fromAsmInternalName(exception));
                    }
                }
                List<Annotation> annotations1 = new ArrayList<>();
                if (methodNode.visibleAnnotations != null) {
                    for (AnnotationNode annotationNode : methodNode.visibleAnnotations) {
                        annotationNode.accept(
                                new AnnoVisitor(annotationNode.desc, annotations1::add));
                    }
                }
                List<String> paramName = null;
                if (methodNode.parameters != null) {
                    paramName = new ArrayList<>();
                    for (ParameterNode parameterNode : methodNode.parameters) {
                        paramName.add(parameterNode.name);
                    }
                }
                JMethod method = new JMethod(jClass, methodNode.name, modifiers1, paramTypes,
                        retType, exceptions, null,
                        AnnotationHolder.make(annotations1), null,
                        paramName,
                        new AsmMethodSource((JSRInlinerAdapter) methodNode, version));
                methods.add(method);
            }

        }
    }

    private String getSimpleName(String binaryName) {
        int lastIndex = binaryName.lastIndexOf(".");
        if (lastIndex == -1) {
            return binaryName;
        }
        return binaryName.substring(lastIndex + 1);
    }

    private static JClass getClassByName(String internalName) {
        return BuildContext.get().getClassByName(getBinaryName(internalName));
    }

    class CVisitor extends ClassVisitor {

        String currentInternalName;

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
            currentInternalName = name;
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
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (outerName != null && Objects.equals(name, currentInternalName)) {
                outerClass = getClassByName(outerName);
                // also, fix modifiers
                modifiers.addAll(fromAsmModifier(access));
            }
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
                fromAsmModifier(access), type, null, AnnotationHolder.make(annotations))));
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MVisitor(access, name, descriptor, exceptions);
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

        private final List<Annotation> annotations;

        private Map<Integer, List<Annotation>> paramAnnotations;

        @Nullable
        private List<String> paramName;

        public MVisitor(int access, String name, String descriptor, String[] exceptions) {
            super(Opcodes.ASM9);
            org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(descriptor);
            this.modifiers = fromAsmModifier(access);
            this.methodName = name;
            this.exceptions = new ArrayList<>();
            if (exceptions != null) {
                for (String exception : exceptions) {
                    this.exceptions.add((ClassType) BuildContext.get().fromAsmInternalName(exception));
                }
            }
            this.paramTypes = new ArrayList<>();
            for (org.objectweb.asm.Type t1 : t.getArgumentTypes()) {
                paramTypes.add(BuildContext.get().fromAsmType(t1));
            }
            this.retType = BuildContext.get().fromAsmType(t.getReturnType());
            this.annotations = new ArrayList<>();
            this.paramAnnotations = null;
        }


        @Override
        public void visitParameter(String name, int access) {
            super.visitParameter(name, access);
            if (paramName == null) {
                paramName = new ArrayList<>();
            }
            paramName.add(name);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return new AnnoVisitor(descriptor, annotations::add);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            // Note: this handle may cause problem for <init>()
            // of inner class (check doc of this function)
            // TODO: fix this
//            return new AnnoVisitor(descriptor, paramAnnotations
//                    .computeIfAbsent(parameter, i -> new ArrayList<>())::add);
            return null;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
//            List<AnnotationHolder> l = new ArrayList<>();
//            for (int i = 0; i < paramTypes.size(); ++i) {
//                List<Annotation> annotations1 = paramAnnotations;
//                AnnotationHolder h = annotations1 == null ?
//                        null : AnnotationHolder.make(annotations1);
//                l.add(h);
//            }
            JMethod method = new JMethod(jClass, methodName, modifiers, paramTypes,
                    retType, exceptions, null,
                    AnnotationHolder.make(annotations), null,
                    paramName,
                    null);
            AsmClassBuilder.this.methods.add(method);
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
