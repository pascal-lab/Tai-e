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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
