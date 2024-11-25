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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import pascal.taie.World;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.util.collection.Maps;

import java.util.Map;

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

       Map<String, IVariableBinding> variableBindingMap = Maps.newMap();

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
