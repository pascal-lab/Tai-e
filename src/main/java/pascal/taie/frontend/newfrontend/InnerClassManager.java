package pascal.taie.frontend.newfrontend;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static pascal.taie.frontend.newfrontend.TypeUtils.JDTTypeToTaieType;
import static pascal.taie.frontend.newfrontend.TypeUtils.getTaieClass;

/**
 * to corporate with Soot, we have to know the real constructor innovation of an inner class.
 */

record InnerClassDescriptor(List<Var> defaultCtorArgs,
                            List<Type> defaultCtorTypes,
                            FieldRef ref) { }

public class InnerClassManager {

    public final static String VAL = "val$";

    private final static InnerClassManager instance = new InnerClassManager();

    /**
     * <p>use this map to record all captured local bindings of inner classes.</p>
     */
    private final Map<JClass, InnerClassDescriptor> innerBindingMap;

    private InnerClassManager() {
        this.innerBindingMap = Maps.newMap();
    }

    public static InnerClassManager get() {
        return instance;
    }

    public static String getCaptureName(String name) {
        return VAL + name;
    }

    /**
     * add a new Inner Class(local class) declared in a method
     */
    public void addLocalClass(TypeDeclaration td,
                              IMethodBinding method,
                              Function<IVariableBinding, Var> map,
                              Var thisVar) {
        JClass jClass = getTaieClass(td.getName().resolveTypeBinding());
        if (jClass == null) {
            throw new NewFrontendException("class " + td + " can't be resolved in tai-e world");
        }
        // note: [td] can only be handled once, so it's safe to add a new object to map
        List<Var> vars = new ArrayList<>();
        vars.add(thisVar);
        List<Type> typeList = new ArrayList<>();
        assert jClass.getOuterClass() != null;
        typeList.add(jClass.getOuterClass().getType());
        td.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName sn) {
                IBinding b = sn.resolveBinding();
                if (b instanceof IVariableBinding v) {
                    if (! v.isField() &&
                            v.getDeclaringMethod().isSubsignature(method)) {
                        vars.add(map.apply(v));
                        typeList.add(JDTTypeToTaieType(v.getType()));
                    }
                }
                return false;
            }
        });
        innerBindingMap.put(jClass, new InnerClassDescriptor(vars, typeList, _getOuterClassRef(jClass)));
    }

    private @Nullable FieldRef _getOuterClassRef(JClass jClass) {
        var fields = jClass.getDeclaredFields();
        for (var i : fields) {
            if (i.getName().startsWith("this$")) {
                return i.getRef();
            }
        }
        return null;
    }

    public @Nullable FieldRef getOuterClassRef(JClass jClass) {
        InnerClassDescriptor descriptor = innerBindingMap.get(jClass);
        if (descriptor == null) {
            var ref = _getOuterClassRef(jClass);
            innerBindingMap.put(jClass, new InnerClassDescriptor(null, null, ref));
            return ref;
        } else {
            return descriptor.ref();
        }
    }

    public List<Type> resolveCtorType(JClass jClass, List<Type> userDefinedType) {
        InnerClassDescriptor descriptor = innerBindingMap.get(jClass);
        if (descriptor != null) {
            return Stream.concat(descriptor.defaultCtorTypes().stream(),
                    userDefinedType.stream()).toList();
        } else {
            return userDefinedType;
        }
    }

    public void resolveConstructor() {
    }
}
