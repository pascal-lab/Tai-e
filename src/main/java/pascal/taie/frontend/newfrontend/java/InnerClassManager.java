package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import pascal.taie.World;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum InnerClassCategory {
    ANONYMOUS,
    LOCAL,
    MEMBER
}


record InnerClassDescriptor(
        ITypeBinding type,
        List<String> synParaNames,
        List<ITypeBinding> synParaTypes,
        boolean isStatic,
        InnerClassCategory category,
        // key is origin name, not field/captured name
        Map<String, IVariableBinding> varBindingMap) { }


/**
 * JLS 8. chap. 8.1.3,
 * <p>
 * An inner class may be a non-static member class (§8.5), a local class (§14.3), or
 * an anonymous class. A member class of an interface is implicitly static
 * (§9.5) so is never considered to be an inner class.
 * </p>
 */
public class InnerClassManager {

    public static final String VAL = "val$";

    public static final String OUTER_THIS = "this$1";

    private static InnerClassManager instance = new InnerClassManager();

    /**
     * <p>use this map to record all captured local bindings of inner classes.</p>
     */
    private final Map<String, InnerClassDescriptor> innerBindingMap;

    private final Map<JClass, JField> outerFieldRef;

    private boolean resolved;

    private InnerClassManager() {
        this.innerBindingMap = Maps.newMap();
        this.outerFieldRef = Maps.newMap();
        resolved = false;
    }

    public static void reset() {
        instance = new InnerClassManager();
    }

    static {
        World.registerResetCallback(InnerClassManager::reset);
    }

    public static InnerClassManager get() {
        return instance;
    }

    public static String getCaptureName(String name) {
        return VAL + name;
    }

    public static String getOrigName(String capName) {
        return capName.substring(VAL.length());
    }

    public void noticeInnerClass(ASTNode typeDeclaration,
                                 ITypeBinding outer,
                                 boolean inStaticContext) {
       ITypeBinding binding = ClassExtractor.getBinding(typeDeclaration);
       boolean needSynThis;
       boolean needSynVal;
       InnerClassCategory category;

       if (outer.isInterface() || binding.isInterface() || TypeUtils.isEnumType(binding)) {
           return;
       }

       if (binding.isMember()) {
           if (TypeUtils.isStatic(binding.getModifiers())) {
               // static member, same with normal class
               return;
           }
           needSynThis = true;
           needSynVal = false;
           category = InnerClassCategory.MEMBER;
       } else {
           if (binding.isAnonymous()) {
               category = InnerClassCategory.ANONYMOUS;
               boolean hasExplicitEnclosedInstance =
                       ((ClassInstanceCreation) typeDeclaration.getParent()).getExpression() != null;
               needSynThis = ! inStaticContext || hasExplicitEnclosedInstance;
           } else {
               assert binding.isLocal();
               category = InnerClassCategory.LOCAL;
               needSynThis = ! inStaticContext;
           }
           needSynVal = true;
       }

       List<String> synParaNames = new ArrayList<>();
       List<ITypeBinding> synParaTypes = new ArrayList<>();

       if (needSynThis) {
           synParaNames.add(OUTER_THIS);
           synParaTypes.add(outer);
       }

       Map<String, IVariableBinding> variableBindingMap = new HashMap<>();
       if (needSynVal) {
           typeDeclaration.accept(new ASTVisitor() {
               @Override
               public boolean visit(SimpleName sn) {
                   IBinding b = sn.resolveBinding();
                   if (b instanceof IVariableBinding v) {
                       if (! v.isField() && v.getDeclaringMethod().getDeclaringClass() != binding) {
                           synParaNames.add(getCaptureName(v.getName()));
                           synParaTypes.add(v.getType());
                           variableBindingMap.put(v.getName(), v);
                       }
                   }
                   return false;
               }
           });
       }

       innerBindingMap.put(JDTStringReps.getBinaryName(binding),
               new InnerClassDescriptor(binding,
                       synParaNames, synParaTypes, ! needSynThis, category, variableBindingMap));
    }

    public FieldRef getOuterClassRef(JClass jClass) {
        JField field = outerFieldRef.get(jClass);
        assert field != null;
        return field.getRef();
    }

    public void noticeOuterClassRef(JClass jClass, JField ref) {
        outerFieldRef.put(jClass, ref);
    }

    InnerClassDescriptor getInnerClassDesc(ITypeBinding binding) {
        return getInnerClassDesc(JDTStringReps.getBinaryName(binding));
    }

    InnerClassDescriptor getInnerClassDesc(String binaryName) {
        if (! resolved) {
            resolveDescriptors();
        }
        return innerBindingMap.get(binaryName);
    }

    boolean isInnerClass(String binaryName) {
        return innerBindingMap.containsKey(binaryName);
    }

    private void resolveDescriptors() {
        innerBindingMap.forEach((k, v) -> {
            fixTransitiveCaptures(v);
        });
        resolved = true;
    }

    /**
     * Consider such situation:
     * <pre>
     *  {@code
     *  void f() {
     *      int k, h = 0;
     *      class A {
     *          void g() {
     *              System.out.println(k);
     *          }
     *      }
     *
     *      class B extends A {
     *          void g() {
     *              super.g();
     *              System.out.println(h);
     *          }
     *      }
     *  }
     *  }
     * </pre>
     * <p>
     * Clearly, both {@code k} and {@code h} should be captured by {@code B},
     * but our algorithm in prev stage can only detect {@code A capture k}, {@code B capture h}.
     * </p>
     * <p>
     * So, after the extraction stage, we perform closure computing to
     * collect all captured variables
     * </p>
     */
    private void fixTransitiveCaptures(InnerClassDescriptor descriptor) {
        ITypeBinding current = descriptor.type();
        ITypeBinding superClass = current.getSuperclass();
        assert superClass == null || ! superClass.isAnonymous();
        while (superClass != null && isLocal(superClass)) {
            addSynArgs(descriptor, getDesc(superClass));
            superClass = superClass.getSuperclass();
        }
    }

    private boolean isLocal(ITypeBinding binding) {
        return binding.isLocal();
    }

    private void addSynArgs(InnerClassDescriptor current, InnerClassDescriptor superClass) {
        boolean isStatic = current.isStatic();
        assert current.isStatic() == superClass.isStatic();
        int start = isStatic ? 0 : 1;
        for (int i = start; i < superClass.synParaNames().size(); ++i) {
            String nowName = superClass.synParaNames().get(i);
            ITypeBinding nowType = superClass.synParaTypes().get(i);
            if (! current.synParaNames().contains(nowName)) {
                current.synParaNames().add(nowName);
                current.synParaTypes().add(nowType);
            }
        }
        superClass.varBindingMap().forEach((k, v) -> current.varBindingMap().put(k, v));
    }

    private InnerClassDescriptor getDesc(ITypeBinding binding) {
        return innerBindingMap.get(JDTStringReps.getBinaryName(binding));
    }
}
