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

package pascal.taie.frontend.java.classes;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.frontend.java.ir.AsmValueUtils;
import pascal.taie.ir.exp.Literal;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.AnnotationElement;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.BooleanElement;
import pascal.taie.language.annotation.ClassElement;
import pascal.taie.language.annotation.DoubleElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.EnumElement;
import pascal.taie.language.annotation.FloatElement;
import pascal.taie.language.annotation.IntElement;
import pascal.taie.language.annotation.LongElement;
import pascal.taie.language.annotation.StringElement;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.generics.GSignatures;
import pascal.taie.language.generics.MethodGSignature;
import pascal.taie.language.generics.ReferenceTypeGSignature;
import pascal.taie.language.type.BytecodeDescriptors;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class BytecodeClassBuilder implements JClassBuilder {

    private final FrontendTypeSystem typeSystem;

    private final JClassLoader loader;

    private final AsmClassSource source;

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

    private ClassGSignature classGSig;

    public BytecodeClassBuilder(FrontendTypeSystem typeSystem,
                                JClassLoader loader,
                                AsmClassSource source,
                                JClass jClass) {
        this.typeSystem = typeSystem;
        this.loader = loader;
        this.source = source;
        this.jClass = jClass;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.annotations = new ArrayList<>();
    }

    @Override
    public void build(JClass jclass) {
        readClassInfo();
        if (jclass != this.jClass) {
            throw new IllegalArgumentException();
        }
        // inject class information to JClass
        jclass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    @Override
    public ClassType getClassType() {
        return typeSystem.getClassType(source.className());
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
        return source.isApp();
    }

    @Override
    public boolean isPhantom() {
        return false;
    }

    @Nullable
    @Override
    public ClassGSignature getGSignature() {
        // TODO: implement this
        return classGSig;
    }

    private void readClassInfo() {
        source.reader().accept(new ClassInfoVisitor(), ClassReader.SKIP_CODE);
    }

    private JClass getClassByInternalName(String internalName) {
        // convert internal name to class name
        String className = org.objectweb.asm.Type.getObjectType(internalName)
                .getClassName();
        // retrieve the class
        return loader.loadClass(className);
    }

    /**
     * Visitor for parsing class-level information.
     * <p>
     * Handles: superclass, interfaces, modifiers, generic signature,
     * outer class (for inner classes), and class annotations.
     */
    private class ClassInfoVisitor extends ClassVisitor {

        /**
         * The internal name of the class being visited.
         * Used to match inner class entries.
         */
        private String internalName;

        private ClassInfoVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            if (superName != null) {
                superClass = getClassByInternalName(superName);
            }
            internalName = name;
            BytecodeClassBuilder.this.interfaces = Arrays.stream(interfaces)
                    .map(BytecodeClassBuilder.this::getClassByInternalName)
                    .toList();
            modifiers = Modifiers.fromAsmClass(access);
            if (signature != null) {
                classGSig = GSignatures.toClassSig(modifiers.contains(Modifier.INTERFACE), signature);
            }
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            BytecodeClassBuilder.this.outerClass = getClassByInternalName(owner);
        }

        /**
         * Visits the InnerClasses attribute entry for inner/nested classes.
         * <p>
         * This method is called for each inner class entry in the class file's
         * InnerClasses attribute. We use it to:
         * <ul>
         *     <li>Identify the outer class of the current class (if it is an inner class)</li>
         *     <li>Retrieve the true access modifiers (e.g., private, static) which are
         *         only available here, not in {@link #visit}</li>
         * </ul>
         */
        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (outerName != null && Objects.equals(name, internalName)) {
                outerClass = getClassByInternalName(outerName);
                // The access flags from visit() are incomplete for inner classes;
                // the true modifiers (private, protected, static) come from here
                modifiers.addAll(Modifiers.fromAsmClass(access));
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return new AnnotationInfoVisitor(descriptor, annotations::add);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            return new FieldInfoVisitor(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            return new MethodInfoVisitor(access, name, descriptor, signature, exceptions);
        }
    }

    /**
     * Visitor for parsing field declarations.
     * <p>
     * Collects field modifiers, type, annotations, generic signature,
     * and constant value. Creates {@link JField} when visiting is complete.
     */
    private class FieldInfoVisitor extends FieldVisitor {

        private final Set<Modifier> modifiers;

        private final String fieldName;

        private final Type type;

        @Nullable
        private final ReferenceTypeGSignature gSignature;

        @Nullable
        private final Literal constantValue;

        private final List<Annotation> annotations = new ArrayList<>();

        private FieldInfoVisitor(int access, String name, String descriptor,
                                 String signature, Object value) {
            super(Opcodes.ASM9);
            this.modifiers = Modifiers.fromAsmField(access);
            this.fieldName = name;
            this.type = typeSystem.fromAsmTypeDesc(descriptor);
            this.gSignature = (signature == null)
                    ? null : GSignatures.toTypeSig(signature);
            this.constantValue = (value == null)
                    ? null : AsmValueUtils.fromObject(typeSystem, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return new AnnotationInfoVisitor(descriptor, annotations::add);
        }

        @Override
        public void visitEnd() {
            JField field = new JField(jClass, fieldName, modifiers, type,
                    gSignature, AnnotationHolder.make(annotations), constantValue);
            BytecodeClassBuilder.this.fields.add(field);
        }
    }

    /**
     * Visitor for parsing method declarations.
     * <p>
     * Collects method signature, modifiers, annotations, parameter names,
     * and exception types. Method body is not parsed here (SKIP_CODE is used).
     */
    private class MethodInfoVisitor extends MethodVisitor {

        private final Set<Modifier> modifiers;

        private final String methodName;

        private final List<ClassType> exceptions;

        private final List<Type> paramTypes;

        private final Type retType;

        private final MethodGSignature gSignature;

        private final List<Annotation> annotations = new ArrayList<>();

        @Nullable
        private List<String> paramNames;

        private final Map<Integer, List<Annotation>> paramAnnotations = Maps.newMap();

        private MethodInfoVisitor(int access, String name, String descriptor,
                                  String signature, String[] exceptions) {
            super(Opcodes.ASM9);
            this.modifiers = Modifiers.fromAsmMethod(access);
            this.methodName = name;
            Pair<List<Type>, Type> mtdType = typeSystem.fromAsmMethodDesc(descriptor);
            this.retType = mtdType.second();
            this.paramTypes = mtdType.first();
            this.gSignature = (signature == null)
                    ? null : GSignatures.toMethodSig(signature);
            this.exceptions = readExceptions(exceptions);
        }

        private List<ClassType> readExceptions(String[] exceptions) {
            if (exceptions == null) {
                return List.of();
            }
            List<ClassType> result = new ArrayList<>(exceptions.length);
            for (String exception : exceptions) {
                result.add((ClassType) typeSystem.fromAsmInternalName(exception));
            }
            return result;
        }

        @Override
        public void visitParameter(String name, int access) {
            if (paramNames == null) {
                paramNames = new ArrayList<>();
            }
            paramNames.add(name);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return new AnnotationInfoVisitor(descriptor, annotations::add);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
                int parameter, String descriptor, boolean visible) {
            // NOTE: this handle may cause problem for <init>()
            // of inner class (check doc of this function)
            // TODO: fix this, and uncomment visitEnd()
            List<Annotation> annos = paramAnnotations
                    .computeIfAbsent(parameter, __ -> new ArrayList<>());
            return new AnnotationInfoVisitor(descriptor, annos::add);
        }

        @Override
        public void visitEnd() {
            List<AnnotationHolder> paramAnnos = null;
            if (!paramAnnotations.isEmpty()) {
                paramAnnos = new ArrayList<>(paramTypes.size());
                for (int i = 0; i < paramTypes.size(); ++i) {
                    paramAnnos.add(AnnotationHolder.make(
                            paramAnnotations.getOrDefault(i, List.of())));
                }
            }
            JMethod method = new JMethod(jClass, methodName, modifiers,
                    paramTypes, retType, exceptions, gSignature,
                    AnnotationHolder.make(annotations), paramAnnos,
                    paramNames, null);
            BytecodeClassBuilder.this.methods.add(method);
        }
    }

    /**
     * Visitor for parsing annotation declarations.
     * <p>
     * Collects annotation elements (name-value pairs) and builds an
     * {@link Annotation} object when visiting is complete.
     * <p>
     * Annotation elements can be:
     * <ul>
     *   <li>Primitives, String, Class - handled by {@link #visit}</li>
     *   <li>Enum constants - handled by {@link #visitEnum}</li>
     *   <li>Nested annotations - handled by {@link #visitAnnotation}</li>
     *   <li>Arrays - handled by {@link #visitArray}</li>
     * </ul>
     */
    private static class AnnotationInfoVisitor extends AnnotationVisitor {

        private final String type;

        private final Map<String, Element> elements = Maps.newHybridMap();

        private final Consumer<Annotation> onComplete;

        private AnnotationInfoVisitor(String descriptor, Consumer<Annotation> onComplete) {
            super(Opcodes.ASM9);
            this.type = BytecodeDescriptors.toTaieTypeDesc(descriptor);
            this.onComplete = onComplete;
        }

        /**
         * primitive array will use this method
         */
        @Override
        public void visit(String name, Object value) {
            elements.put(name, toElement(value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            String enumType = BytecodeDescriptors.toTaieTypeDesc(descriptor);
            elements.put(name, new EnumElement(enumType, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return new AnnotationInfoVisitor(descriptor,
                    anno -> elements.put(name, new AnnotationElement(anno)));
        }

        /**
         * Non-primitive array will use this method
         */
        @Override
        public AnnotationVisitor visitArray(String name) {
            return new AnnotationArrayVisitor(
                    elems -> elements.put(name, new ArrayElement(elems)));
        }

        @Override
        public void visitEnd() {
            onComplete.accept(new Annotation(type, elements));
        }
    }

    /**
     * Visitor for parsing annotation array elements.
     * <p>
     * Used when an annotation element is an array of non-primitive values
     * (e.g., array of enums, Class objects, or nested annotations).
     * Primitive arrays are handled directly by {@link AnnotationInfoVisitor#visit}.
     */
    private static class AnnotationArrayVisitor extends AnnotationVisitor {

        private final List<Element> elements = new ArrayList<>();

        private final Consumer<List<Element>> onComplete;

        private AnnotationArrayVisitor(Consumer<List<Element>> onComplete) {
            super(Opcodes.ASM9);
            this.onComplete = onComplete;
        }

        /**
         * Visits a primitive, String, or Class array element.
         */
        @Override
        public void visit(String name, Object value) {
            elements.add(toElement(value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            String enumType = BytecodeDescriptors.toTaieTypeDesc(descriptor);
            elements.add(new EnumElement(enumType, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return new AnnotationInfoVisitor(descriptor,
                    anno -> elements.add(new AnnotationElement(anno)));
        }

        @Override
        public void visitEnd() {
            onComplete.accept(elements);
        }
    }

    /**
     * Convert object to tai-e Annotation representation.
     *
     * @param o object, should be boxed primitive type OR string OR array OR ASM type
     */
    private static Element toElement(Object o) {
        if (o instanceof Byte b) {
            return new IntElement(b);
        } else if (o instanceof Boolean b) {
            return new BooleanElement(b);
        } else if (o instanceof Character c) {
            return new IntElement(c);
        } else if (o instanceof Short s) {
            return new IntElement(s);
        } else if (o instanceof Integer i) {
            return new IntElement(i);
        } else if (o instanceof Long l) {
            return new LongElement(l);
        } else if (o instanceof Float f) {
            return new FloatElement(f);
        } else if (o instanceof Double d) {
            return new DoubleElement(d);
        } else if (o instanceof String s) {
            return new StringElement(s);
        } else if (o.getClass().isArray()) {
            List<Element> elements = new ArrayList<>();
            for (int i = 0; i < Array.getLength(o); ++i) {
                elements.add(toElement(Array.get(o, i)));
            }
            return new ArrayElement(elements);
        } else if (o instanceof org.objectweb.asm.Type c) {
            return toClassElement(c);
        } else {
            throw new IllegalArgumentException(
                    o + " is not a valid annotation element");
        }
    }

    private static Element toClassElement(org.objectweb.asm.Type c) {
        if (c.getDescriptor().equals("V")) {
            // This is due to the abuse of notations in the classfile format.
            // According to JVM Spec. 4.3, "V" is an invalid descriptor.
            // You cannot define a value with type void; void can only be used
            // as the return type in method descriptors.
            // However, in the "class_info_index" defined in JVM Spec. 4.7.16, "V" is permitted.
            // For example:
            //     @MyAnnotation(type = void.class)
            // This will be compiled to:
            //    RuntimeVisibleAnnotations:
            //        MyAnnotation(
            //          type=class V
            //        )
            // Therefore, we need to handle this case specially, as "V" is not
            // a valid descriptor and cannot be passed to StringReps#toTaieTypeDesc.
            return new ClassElement("void");
        } else {
            return new ClassElement(BytecodeDescriptors.toTaieTypeDesc(c.getDescriptor()));
        }
    }
}
