package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import pascal.taie.World;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LambdaManager {

    public record LambdaDescriptor(LambdaExpression ast,
                                   IMethodBinding lambda,
                                   IMethodBinding functionalInterface,
                                   ITypeBinding belongingToClass,
                                   IVariableBinding[] jdtCaptured,
                                   Set<IVariableBinding> allCaptured,
                                   boolean isStatic) {
        void addCaptured(IVariableBinding var) {
            allCaptured.add(var);
        }

        Pair<List<String>, List<ITypeBinding>> computeSubSig() {
            // captured + args
            List<String> argNames = new ArrayList<>();
            List<ITypeBinding> argTypes = new ArrayList<>();
            for (IVariableBinding var : computeArgBindings()) {
                argNames.add(var.getName());
                argTypes.add(var.getType());
            }
            return new Pair<>(argNames, argTypes);
        }

        List<IVariableBinding> computeArgBindings() {
            List<IVariableBinding> argBindings = new ArrayList<>(allCaptured);
            for (Object _svd : ast.parameters()) {
                if (_svd instanceof SingleVariableDeclaration svd) {
                    argBindings.add(svd.resolveBinding());
                } else if (_svd instanceof VariableDeclarationFragment fragment) {
                    argBindings.add(fragment.resolveBinding());
                } else {
                    throw new UnsupportedOperationException();
                }
            }
            return argBindings;
        }
    }

    private static LambdaManager manager;

    private final Map<IMethodBinding, LambdaDescriptor> lambdaMap;

    /**
     * Only contains lambdas that are directly contained in the class
     * i.e., lambdas which has innermost enclosing class as the class
     */
    private final MultiMap<ITypeBinding, LambdaDescriptor> containingLambdas;

    private final Map<LambdaDescriptor, JMethod> lambdaMethodMap;

    static LambdaManager get() {
        if (manager == null) {
            manager = new LambdaManager();
        }
        return manager;
    }

    static {
        World.registerResetCallback(() -> {
            manager = null;
        });
    }

    private LambdaManager() {
        lambdaMap = Maps.newMap();
        containingLambdas = Maps.newMultiMap();
        lambdaMethodMap = Maps.newMap();
    }

    public void addLambda(LambdaExpression ast,
                          IMethodBinding lambda,
                          IMethodBinding functionalInterface,
                          ITypeBinding belongingToClass,
                          boolean isStatic) {
        IVariableBinding[] capturedVars = lambda.getSyntheticOuterLocals();
        LambdaDescriptor desc = new LambdaDescriptor(ast, lambda,
                functionalInterface, belongingToClass, capturedVars, Sets.newSet(), isStatic);
        for (IVariableBinding var : processJDTCaptured(capturedVars)) {
            desc.addCaptured(var);
        }
        lambdaMap.put(lambda, desc);
        containingLambdas.put(belongingToClass, desc);
    }

    private Set<IVariableBinding> processJDTCaptured(IVariableBinding[] jdtCaptured) {
        Set<IVariableBinding> captured = Sets.newSet();
        for (IVariableBinding var : jdtCaptured) {
            captured.add(obtainOriginalVar(var));
        }
        return captured;
    }

    /**
     * Use a very dirty way to obtain the original variable, may not work in future
     * @see <a href="https://github.com/eclipse-jdt/eclipse.jdt.core/issues/2184">JDT issue</a>
     * @param var the variable binding returned by JDT
     * @return the original variable
     */
    static IVariableBinding obtainOriginalVar(IVariableBinding var) {
        // use reflect to get
        // 1. the resolver
        // 2. the binding (from jdt.internal.compiler.lookup)
        try {
            Class<?> variableBinding = var.getClass();
            Field binding = variableBinding.getDeclaredField("binding");
            binding.setAccessible(true);
            Object bindingObj = binding.get(var);
            if (!(bindingObj instanceof SyntheticArgumentBinding sab)) {
                throw new UnsupportedOperationException();
            }
            Binding originalBinding = sab.actualOuterLocalVariable;
            Field resolver = variableBinding.getDeclaredField("resolver");
            resolver.setAccessible(true);
            Object resolverObj = resolver.get(var);
            Class<?> resolverKlass = resolverObj.getClass();
            Method getBinding = resolverKlass.getDeclaredMethod("getBinding", Binding.class);
            getBinding.setAccessible(true);
            Object ret = getBinding.invoke(resolverObj, originalBinding);
            return (IVariableBinding) ret;
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public Set<LambdaDescriptor> getLambdasInClass(ITypeBinding clazz) {
        return containingLambdas.get(clazz);
    }

    public LambdaDescriptor getDescriptor(IMethodBinding lambda) {
        return lambdaMap.get(lambda);
    }

    public boolean isLambda(IMethodBinding method) {
        return lambdaMap.containsKey(method);
    }

    public void setLambdaMethod(LambdaDescriptor desc, JMethod method) {
        lambdaMethodMap.put(desc, method);
    }

    public JMethod getLambdaMethod(LambdaDescriptor desc) {
        return lambdaMethodMap.get(desc);
    }
}
