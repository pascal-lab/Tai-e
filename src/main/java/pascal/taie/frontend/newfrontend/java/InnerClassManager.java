package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import pascal.taie.World;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

enum InnerClassCategory {
    ANONYMOUS,
    LOCAL,
    MEMBER
}


final class InnerClassDescriptor {
    private final ITypeBinding type;
    private List<String> synParaNames;
    private List<ITypeBinding> synParaTypes;
    private final boolean isStatic;
    private final InnerClassCategory category;
    private final Map<String, IVariableBinding> varBindingMap;
    private final Set<IVariableBinding> capturedVars;
    private final ITypeBinding outerClass;

    /**
     * Only used for anonymous class
     */
    private final ITypeBinding explicitEnclosedInstance;

    InnerClassDescriptor(
            ITypeBinding type,
            boolean isStatic,
            InnerClassCategory category,
            // key is origin name, not field/captured name
            Map<String, IVariableBinding> varBindingMap,
            ITypeBinding outerClass,
            ITypeBinding explicitEnclosedInstance) {
        this.type = type;
        this.isStatic = isStatic;
        this.category = category;
        this.varBindingMap = varBindingMap;
        this.capturedVars = Sets.newSet();
        this.outerClass = outerClass;
        this.explicitEnclosedInstance = explicitEnclosedInstance;
    }

    public ITypeBinding type() {
        return type;
    }

    public List<String> synParaNames() {
        return synParaNames;
    }

    public List<ITypeBinding> synParaTypes() {
        return synParaTypes;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public InnerClassCategory category() {
        return category;
    }

    public Map<String, IVariableBinding> varBindingMap() {
        return varBindingMap;
    }

    public void addNewCapture(IVariableBinding v) {
        capturedVars.add(v);
    }

    public Set<IVariableBinding> capturedVars() {
        return capturedVars;
    }

    public void resolveSynPara(Set<IVariableBinding> directCaptures) {
        // should always be null, or it must be a bug
        // (we should not call this method twice)
        assert synParaNames == null && synParaTypes == null;
        synParaNames = new ArrayList<>();
        synParaTypes = new ArrayList<>();
        if (!isStatic) {
            // add `this` as the first parameter
            synParaNames.add(InnerClassManager.OUTER_THIS);
            synParaTypes.add(outerClass);
        }
        for (IVariableBinding v : directCaptures) {
            String name = InnerClassManager.getCaptureName(v.getName());
            synParaNames.add(name);
            synParaTypes.add(v.getType());
            varBindingMap().put(v.getName(), v);
        }
    }

    boolean isDirectlyCaptured(IVariableBinding v) {
        return varBindingMap().containsValue(v);
    }

    public ITypeBinding getExplicitEnclosedInstance() {
        return explicitEnclosedInstance;
    }

    public ITypeBinding getOuterClass() {
        return outerClass;
    }

}


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
       InnerClassCategory category;
       ITypeBinding explicitEnclosedInstance = null;

       if (outer.isInterface() || binding.isInterface() || TypeUtils.isEnumType(binding)) {
           return;
       }

       if (binding.isMember()) {
           if (TypeUtils.isStatic(binding.getModifiers())) {
               // static member, same with normal class
               return;
           }
           needSynThis = true;
           category = InnerClassCategory.MEMBER;
       } else {
           if (binding.isAnonymous()) {
               category = InnerClassCategory.ANONYMOUS;
               Expression expression = ((ClassInstanceCreation) typeDeclaration.getParent()).getExpression();
               boolean hasExplicitEnclosedInstance = expression != null;
               // it's something like
               // class A { class AInner { ... } }
               // static f() {
               //     a = new A();
               //     b = new a.AInner() { ... };
               // }
               needSynThis = ! inStaticContext;
               explicitEnclosedInstance = hasExplicitEnclosedInstance ?
                       expression.resolveTypeBinding() : null;
           } else {
               assert binding.isLocal();
               category = InnerClassCategory.LOCAL;
               needSynThis = ! inStaticContext;
           }
       }

       Map<String, IVariableBinding> variableBindingMap = new HashMap<>();

       innerBindingMap.put(JDTStringReps.getBinaryName(binding),
               new InnerClassDescriptor(binding, !needSynThis, category,
                       variableBindingMap, outer, explicitEnclosedInstance));
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
        return innerBindingMap.get(binaryName);
    }

    boolean isInnerClass(String binaryName) {
        return innerBindingMap.containsKey(binaryName);
    }

    public static boolean isLocal(ITypeBinding binding) {
        return binding.isLocal();
    }

    private InnerClassDescriptor getDesc(ITypeBinding binding) {
        return innerBindingMap.get(JDTStringReps.getBinaryName(binding));
    }

    public void noticeCaptureVariableBinding(IVariableBinding v, ITypeBinding currentClass) {
        InnerClassDescriptor desc = getDesc(currentClass);
        assert desc != null;
        desc.addNewCapture(v);
    }

    public void applySubsetCaptureRule(ITypeBinding c1, ITypeBinding c2) {
        InnerClassDescriptor desc1 = getDesc(c1);
        InnerClassDescriptor desc2 = getDesc(c2);
        assert desc1 != null && desc2 != null;
        for (IVariableBinding v : desc1.capturedVars()) {
            desc2.addNewCapture(v);
        }
    }
}
