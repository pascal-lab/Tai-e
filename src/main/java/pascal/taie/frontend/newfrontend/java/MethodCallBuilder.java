package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.Type;

import java.util.ArrayList;
import java.util.List;

public class MethodCallBuilder {

    public static MethodRef getMethodRef(IMethodBinding binding) {
        ITypeBinding declClass = binding.getDeclaringClass();
        JClass jClass = TypeUtils.getTaieClass(declClass);
        Type retType = TypeUtils.JDTTypeToTaieType(binding.getReturnType());
        List<Type> paras = new ArrayList<>();
        for (var i : binding.getParameterTypes()) {
             paras.add(TypeUtils.JDTTypeToTaieType(i));
        }
        return MethodRef.get(jClass, binding.getName(), paras, retType, Modifier.isStatic(binding.getModifiers()));
    }

    public static MethodRef getInitRef(IMethodBinding binding) {
        ITypeBinding declClass = binding.getDeclaringClass();
        JClass jClass = TypeUtils.getTaieClass(declClass);
        Type retType = TypeUtils.JDTTypeToTaieType(binding.getReturnType());
        List<Type> paras = new ArrayList<>();
        for (var i : binding.getParameterTypes()) {
            paras.add(TypeUtils.JDTTypeToTaieType(i));
        }
        return MethodRef.get(jClass, MethodNames.INIT, paras, retType, Modifier.isStatic(binding.getModifiers()));
    }
}
