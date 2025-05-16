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

package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import pascal.taie.frontend.newfrontend.FrontendContext;
import pascal.taie.frontend.newfrontend.source.JavaMethodSource;
import pascal.taie.frontend.newfrontend.source.JavaSource;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaClassBuilder implements JClassBuilder  {

    private final JClass jClass;

    private final JavaSource sourceFile;

    private Set<Modifier> modifiers;

    private String simpleName;

    private JClass superClass;

    private List<JClass> interfaces;

    private JClass outerClass;

    private final List<JField> fields;

    private final List<JMethod> methods;

    private InnerClassDescriptor descriptor;

    private ITypeBinding binding;

    private ASTNode typeDeclaration;

    private AnnotationHolder holder;

    public JavaClassBuilder(JavaSource sourceFile, JClass jClass) {
        this.sourceFile = sourceFile;
        this.jClass = jClass;
        fields = new ArrayList<>();
        methods = new ArrayList<>();
    }


    // TODO: handle annotation
    public void build() {
        typeDeclaration = sourceFile.getTypeDeclaration();
        CompilationUnit cu = sourceFile.getUnit();
        AtomicBoolean meetCtor = new AtomicBoolean(false);
        binding = ClassExtractor.getBinding(typeDeclaration);
        modifiers = TypeUtils.computeModifier(binding);
        simpleName = binding.getName();
        interfaces = Arrays.stream(binding.getInterfaces())
                .map(t -> (ClassType) TypeUtils.JDTTypeToTaieType(t))
                .map(ClassType::getJClass)
                .toList();
        superClass = TypeUtils.getSuperClass(binding);
        descriptor = InnerClassManager.get()
                .getInnerClassDesc(binding);
        if (descriptor == null) {
            outerClass = null;
        } else {
            outerClass = TypeUtils.getJClass(descriptor.getOuterClass());
        }
        holder = TypeUtils.getAnnotations(binding);
        List<EnumConstantDeclaration> enumConstDecls = new ArrayList<>();

        typeDeclaration.accept(new ASTVisitor() {

            @Override
            public boolean visit(Initializer node) {
                int modifiers = node.getModifiers();
                if (TypeUtils.isStatic(modifiers)) {
                    sourceFile.addNewCinit(new StaticInit(node));
                } else {
                    sourceFile.addNewInit(new BlockInit(node));
                }
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(MethodDeclaration node) {
                IMethodBinding methodBinding = node.resolveBinding();
                Set<Modifier> sourceModifier = TypeUtils.fromJDTModifier(methodBinding.getModifiers());
                Set<Modifier> current = Sets.newSet(sourceModifier);
                if (node.getBody() == null) {
                    // abstract method
                    current.add(Modifier.ABSTRACT);
                }
                assert methodBinding.getDeclaringClass() == binding;
                String name;
                if (methodBinding.isConstructor()) {
                    meetCtor.set(true);
                    name = MethodNames.INIT;
                } else {
                    name = methodBinding.getName();
                }
                MethodType methodType = TypeUtils.getMethodType(methodBinding);
                List<SingleVariableDeclaration> svds = node.parameters();
                List<String> paraNames = new ArrayList<>();
                for (SingleVariableDeclaration svd : svds) {
                    String paraName = svd.getName().getIdentifier();
                    paraNames.add(paraName);
                }
                List<Type> paraTypes = methodType.getParamTypes();
                if (methodBinding.isConstructor()) {
                    Pair<List<Type>, List<String>> paraTypeNames = getSynCtorTypeNames(paraTypes, paraNames);
                    paraTypes = paraTypeNames.first();
                    paraNames = paraTypeNames.second();
                }
                JMethod method = new JMethod(jClass, name, current, paraTypes,
                        methodType.getReturnType(), TypeUtils.toClassTypes(methodBinding.getExceptionTypes()),
                        null,
                        TypeUtils.getAnnotations(methodBinding), null, paraNames,
                        new JavaMethodSource(cu, node, sourceFile));
                methods.add(method);
                return false;
            }

            @Override
            public boolean visit(TypeDeclaration node) {
                return node.resolveBinding() == binding;
            }

            @Override
            public boolean visit(AnonymousClassDeclaration node) {
                return node.resolveBinding() == binding;
            }

            @Override
            public boolean visit(EnumDeclaration node) {
                return node.resolveBinding() == binding;
            }

            @Override
            public boolean visit(AnnotationTypeDeclaration node) {
                return node.resolveBinding() == binding;
            }

            @Override
            public boolean visit(FieldDeclaration node) {
                Set<Modifier> current = TypeUtils.fromJDTModifier(node.getModifiers());
                List<VariableDeclarationFragment> l = node.fragments();
                for (VariableDeclarationFragment fragment : l) {
                    SimpleName name = fragment.getName();
                    IVariableBinding binding = fragment.resolveBinding();
                    Type type = TypeUtils.JDTTypeToTaieType(binding.getType());
                    JField field = new JField(jClass, name.getIdentifier(),
                            current, type, null,
                            TypeUtils.getAnnotations(binding), null);
                    fields.add(field);

                    Expression init = fragment.getInitializer();
                    if (init != null) {
                        FieldInit init1 = new FieldInit(field, init);
                        if (field.isStatic()) {
                            sourceFile.addNewCinit(init1);
                        } else {
                            sourceFile.addNewInit(init1);
                        }
                    }
                }
                return false;
            }

            @Override
            public boolean visit(EnumConstantDeclaration node) {
                assert binding.isEnum();
                enumConstDecls.add(node);
                return false;
            }
        });

        if (! meetCtor.get() && ! binding.isInterface()) {
            // add a default non-arg ctor
            Pair<List<Type>, List<String>> paraTypeNames = getSynCtorTypeNames(List.of(), List.of());
            List<Type> paraTypes = paraTypeNames.first();
            List<String> paraNames = paraTypeNames.second();
            JMethod method = new JMethod(jClass, MethodNames.INIT,
                    TypeUtils.copyVisuality(getModifiers()),
                    paraTypes,
                    VoidType.VOID,
                    List.of(),
                    null,
                    AnnotationHolder.emptyHolder(),
                    null,
                    paraNames,
                    new JavaMethodSource(cu, null, sourceFile));
            methods.add(method);
        }

        if (descriptor != null) {
            List<Type> synParaTypes = TypeUtils.fromJDTTypeList(descriptor.synParaTypes().stream());
            for (int i = 0; i < synParaTypes.size(); ++i) {
                Type type = synParaTypes.get(i);
                String name = descriptor.synParaNames().get(i);
                JField f = new JField(jClass, name,
                            Set.of(Modifier.FINAL, Modifier.SYNTHETIC), type,
                        null, AnnotationHolder.emptyHolder(), null);
                if (name.startsWith("this$")) {
                    InnerClassManager.get().noticeOuterClassRef(jClass, f);
                }
                fields.add(f);
            }
        }

        if (binding.isEnum() && ! binding.isAnonymous()) {
            JavaMethodSource empty = new JavaMethodSource(cu, null, sourceFile);
            if (!enumConstDecls.isEmpty()) {
                for (EnumConstantDeclaration decl : enumConstDecls) {
                    String name = decl.getName().toString();
                    Type t = getClassType();
                    JField f = new JField(jClass, name,
                            Set.of(Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL, Modifier.ENUM),
                            t, null, AnnotationHolder.emptyHolder(), null);
                    fields.add(f);
                }
                sourceFile.addNewCinit(new EnumInit(enumConstDecls));
            }
            ArrayType values = FrontendContext.get().getTypeSystem().getArrayType(getClassType(), 1);
            fields.add(new JField(jClass, TypeUtils.ENUM_VALUES,
                    Set.of(Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL, Modifier.SYNTHETIC),
                    values, null, AnnotationHolder.emptyHolder(), null));

            methods.add(new JMethod(jClass, TypeUtils.ENUM_METHOD_VALUES,
                    Set.of(Modifier.PUBLIC, Modifier.STATIC),
                    List.of(), values, List.of(), null, AnnotationHolder.emptyHolder(),
                    null, List.of(),
                    empty));

            methods.add(new JMethod(jClass, TypeUtils.ENUM_METHOD_VALUE_OF,
                    Set.of(Modifier.PUBLIC, Modifier.STATIC),
                    List.of(TypeUtils.getStringType()), getClassType(), List.of(), null,
                    AnnotationHolder.emptyHolder(), null,
                    List.of(TypeUtils.getAnonymousSynCtorArgName(0)),
                    empty));
        }

        if (! sourceFile.getClassInits().isEmpty()) {
            JMethod method = new JMethod(jClass, MethodNames.CLINIT,
                    Set.of(Modifier.STATIC), List.of(), VoidType.VOID, List.of(), null,
                    AnnotationHolder.emptyHolder(), null, List.of(),
                    new JavaMethodSource(cu, null, sourceFile));
            methods.add(method);
        }

        int count = 0;
        for (LambdaManager.LambdaDescriptor lam : LambdaManager.get().getLambdasInClass(binding)) {
            Pair<List<String>, List<ITypeBinding>> subSig = lam.computeSubSig();
            List<String> paraNames = subSig.first();
            List<Type> paraTypes = TypeUtils.fromJDTTypeList(subSig.second().stream());
            Type retType = TypeUtils.JDTTypeToTaieType(lam.lambda().getReturnType());
            String methodName = "lambda$" + count++;
            Set<Modifier> modifiers = lam.isStatic() ?
                    Set.of(Modifier.STATIC, Modifier.PRIVATE, Modifier.SYNTHETIC) :
                    Set.of(Modifier.PRIVATE, Modifier.SYNTHETIC);
            JMethod method = new JMethod(jClass, methodName, modifiers,
                    paraTypes, retType, List.of(), null,
                    AnnotationHolder.emptyHolder(), null, paraNames,
                    new JavaMethodSource(cu, lam.ast(), sourceFile));
            methods.add(method);
            LambdaManager.get().setLambdaMethod(lam, method);
        }
    }

    @Override
    public void build(JClass jclass) {
        build();
        assert jclass == this.jClass;
        jClass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public ClassType getClassType() {
        return FrontendContext.get().getTypeSystem()
                .getClassType(sourceFile.getClassName());
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
        return holder;
    }

    @Override
    public boolean isApplication() {
        return sourceFile.isApplication();
    }

    @Override
    public boolean isPhantom() {
        return false;
    }

    @Nullable
    @Override
    public ClassGSignature getGSignature() {
        // TODO: complete it
        return null;
    }

    private Pair<List<Type>, List<String>> getSynCtorTypeNames(List<Type> paraTypes,
                                                               List<String> paraNames) {
        if (binding.isEnum()) {
            if (binding.isAnonymous()) {
                assert paraTypes.isEmpty() && paraNames.isEmpty();
                assert typeDeclaration.getParent() instanceof EnumConstantDeclaration;
                EnumConstantDeclaration constDecl = (EnumConstantDeclaration) typeDeclaration.getParent();
                var typeNames = getAnonymousParaArgs(constDecl.resolveConstructorBinding().getParameterTypes(), 2);
                paraTypes = typeNames.first();
                paraNames = typeNames.second();
            }
            paraTypes = TypeUtils.getEnumCtorArgType(paraTypes);
            paraNames = TypeUtils.getAnonymousSynCtorArgName(paraNames);
        } else if (descriptor != null) {
            List<Type> synParaTypes = TypeUtils.fromJDTTypeList(descriptor.synParaTypes().stream());
            if (binding.isAnonymous()) {
                assert paraTypes.isEmpty() && paraNames.isEmpty();
                assert typeDeclaration.getParent() instanceof ClassInstanceCreation;
                ClassInstanceCreation creation = (ClassInstanceCreation) typeDeclaration.getParent();
                ITypeBinding[] resolvedParaTypes =
                        creation.resolveConstructorBinding().getParameterTypes();
                ITypeBinding[] fixedParaTypes;
                if (creation.getExpression() == null) {
                    fixedParaTypes = resolvedParaTypes;
                } else {
                    fixedParaTypes = new ITypeBinding[resolvedParaTypes.length + 1];
                    fixedParaTypes[0] = creation.getExpression().resolveTypeBinding();
                    System.arraycopy(resolvedParaTypes, 0,
                            fixedParaTypes, 1, resolvedParaTypes.length);
                }
                var typeNames = getAnonymousParaArgs(fixedParaTypes, synParaTypes.size());
                paraTypes = typeNames.first();
                paraNames = typeNames.second();
            }
            paraTypes = TypeUtils.addList(synParaTypes, paraTypes);
            paraNames = TypeUtils.addList(descriptor.synParaNames(), paraNames);
        }

        return new Pair<>(paraTypes, paraNames);
    }

    private Pair<List<Type>, List<String>> getAnonymousParaArgs(ITypeBinding[] bindings, int synArgLength) {
        List<Type> paraTypes = new ArrayList<>();
        List<String> paraNames = new ArrayList<>();
        for (int i = 0; i < bindings.length; ++i) {
            ITypeBinding binding1 = bindings[i];
            paraTypes.add(TypeUtils.JDTTypeToTaieType(binding1));
            paraNames.add(TypeUtils.getAnonymousSynCtorArgName(i + synArgLength));
        }
        return new Pair<>(paraTypes, paraNames);
    }
}
