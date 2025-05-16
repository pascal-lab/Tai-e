/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.frontend.newfrontend.hierarchy;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import pascal.taie.frontend.newfrontend.FrontendContext;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.frontend.newfrontend.main.NewFrontendComponent;
import pascal.taie.frontend.newfrontend.source.AsmSource;
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
import pascal.taie.language.generics.GSignatures;
import pascal.taie.language.generics.MethodGSignature;
import pascal.taie.language.generics.ReferenceTypeGSignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static pascal.taie.frontend.newfrontend.Utils.fromAsmClassModifier;
import static pascal.taie.frontend.newfrontend.Utils.fromAsmFieldModifier;
import static pascal.taie.frontend.newfrontend.Utils.fromAsmMethodModifier;
import static pascal.taie.frontend.newfrontend.Utils.getBinaryName;
import static pascal.taie.frontend.newfrontend.Utils.toElement;

public class BytecodeClassBuilder extends NewFrontendComponent
        implements JClassBuilder {

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

    private ClassGSignature klassGSig;

    private final int version;

    public BytecodeClassBuilder(FrontendContext context, AsmSource source, JClass jClass) {
        super(context);
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
        return typeSystem().getClassType(source.getClassName());
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
        return klassGSig;
    }

    private void buildAll() {
        CVisitor visitor = new CVisitor();
        source.r().accept(visitor, ClassReader.SKIP_CODE);
    }

    private String getSimpleName(String binaryName) {
        int lastIndex = binaryName.lastIndexOf(".");
        if (lastIndex == -1) {
            return binaryName;
        }
        return binaryName.substring(lastIndex + 1);
    }

    private JClass getClassByName(String internalName) {
        return ctx().getClassByName(getBinaryName(internalName));
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
            BytecodeClassBuilder.this.interfaces = Arrays.stream(interfaces)
                    .map(BytecodeClassBuilder.this::getClassByName)
                    .toList();

            modifiers = fromAsmClassModifier(access);
            if (signature != null) {
                klassGSig = GSignatures.toClassSig(modifiers.contains(Modifier.INTERFACE), signature);
            }
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            BytecodeClassBuilder.this.outerClass = getClassByName(owner);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (outerName != null && Objects.equals(name, currentInternalName)) {
                outerClass = getClassByName(outerName);
                // also, fix modifiers
                modifiers.addAll(fromAsmClassModifier(access));
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
            Type type = ctx().fromAsmType(descriptor);
            ReferenceTypeGSignature gSignature;
            if (signature != null) {
                gSignature = GSignatures.toTypeSig(signature);
            } else {
                gSignature = null;
            }
            return new FVisitor(annotations -> fields.add(
                    new JField(jClass, name, fromAsmFieldModifier(access), type, gSignature,
                            AnnotationHolder.make(annotations),
                            value == null ? null : Utils.fromObject(ctx(), value))));
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MVisitor(access, name, descriptor, exceptions,
                    signature == null ? null : GSignatures.toMethodSig(signature));
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

        private final MethodGSignature gSignature;

        public MVisitor(int access, String name, String descriptor, String[] exceptions, MethodGSignature sig) {
            super(Opcodes.ASM9);
            this.modifiers = fromAsmMethodModifier(access);
            this.methodName = name;
            this.exceptions = new ArrayList<>();
            if (exceptions != null) {
                for (String exception : exceptions) {
                    this.exceptions.add((ClassType) ctx().fromAsmInternalName(exception));
                }
            }
            Pair<List<Type>, Type> mtdType = ctx().fromAsmMethodType(descriptor);
            this.retType = mtdType.second();
            this.paramTypes = mtdType.first();
            this.annotations = new ArrayList<>();
            this.paramAnnotations = Maps.newMap();
            this.gSignature = sig;
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
            return new AnnoVisitor(descriptor, paramAnnotations
                    .computeIfAbsent(parameter, i -> new ArrayList<>())::add);
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
                    retType, exceptions, gSignature,
                    AnnotationHolder.make(annotations), null,
                    paramName,
                    null);
            BytecodeClassBuilder.this.methods.add(method);
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
            this.type = StringReps.toTaieTypeDesc(descriptor);
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
