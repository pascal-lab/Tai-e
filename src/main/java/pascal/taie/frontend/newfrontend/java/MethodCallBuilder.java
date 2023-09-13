package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

import java.util.ArrayList;
import java.util.List;

public class MethodCallBuilder {

    public static MethodRef getMethodRef(IMethodBinding binding) {
        ITypeBinding declClass = binding.getDeclaringClass();
        JClass jClass = TypeUtils.getTaieClass(declClass);
        Type retType = TypeUtils.JDTTypeToTaieType(binding.getReturnType());

        if (binding.getName().equals("clone") && retType instanceof ArrayType) {
            ClassType objType = TypeUtils.getClassByName(ClassNames.OBJECT);
            jClass = objType.getJClass();
            retType = objType;
        }
        List<Type> paras = new ArrayList<>();
        for (var i : binding.getParameterTypes()) {
             paras.add(TypeUtils.JDTTypeToTaieType(i));
        }
        return MethodRef.get(jClass, binding.getName(), paras, retType, Modifier.isStatic(binding.getModifiers()));
    }

    public static MethodRef getInitRef(IMethodBinding binding) {
        ITypeBinding declClass = binding.getDeclaringClass();
        JClass jClass = TypeUtils.getTaieClass(declClass);
        Type retType = VoidType.VOID;
        List<Type> paras = new ArrayList<>();
        for (var i : binding.getParameterTypes()) {
            paras.add(TypeUtils.JDTTypeToTaieType(i));
        }
        paras = TypeUtils.getInitParamTypeWithSyn(paras, binding.getDeclaringClass());
        assert jClass != null;
        return MethodRef.get(jClass, MethodNames.INIT, paras, retType, false);
    }

    public static MethodRef getNonArgInitRef(JClass jClass, ITypeBinding binding) {
        assert jClass != null;
        Type retType = VoidType.VOID;
        List<Type> paras = TypeUtils.getInitParamTypeWithSyn(List.of(), binding);
        return MethodRef.get(jClass, MethodNames.INIT, paras, retType, false);
    }

    public static MethodRef getEnumInitRef(IMethodBinding binding, boolean needSuper) {
        ITypeBinding declClass = binding.getDeclaringClass();
        JClass jClass = needSuper ? TypeUtils.getSuperClass(declClass) :
                TypeUtils.getTaieClass(declClass);
        List<Type> paras = TypeUtils.getEnumCtorArgType(
                TypeUtils.fromJDTTypeList(binding.getParameterTypes()));
        return MethodRef.get(jClass, MethodNames.INIT, paras, VoidType.VOID, false);
    }

    public static IMethodBinding resolveSuperInitForAnonymous(IMethodBinding ctor) {
        ITypeBinding superKlass = ctor.getDeclaringClass().getSuperclass();
        for (IMethodBinding methodBinding : superKlass.getDeclaredMethods()) {
            if (methodBinding.isConstructor()) {
                if (isSameParaType(methodBinding.getParameterTypes(),
                        ctor.getParameterTypes())) {
                    return methodBinding;
                }
            }
        }

        throw new UnsupportedOperationException();
    }

    public static boolean isSameParaType(ITypeBinding[] type1, ITypeBinding[] type2) {
        if (type1.length != type2.length) {
            return false;
        }

        for (int i = 0; i < type1.length; ++i) {
            ITypeBinding t1 = type1[i];
            ITypeBinding t2 = type2[i];

            if (! t1.getErasure().equals(t2.getErasure())) {
                return false;
            }
        }

        return true;
    }
}
