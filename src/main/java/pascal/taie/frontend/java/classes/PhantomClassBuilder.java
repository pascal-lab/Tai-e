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

import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PhantomClassBuilder implements JClassBuilder {

    private final TypeSystem typeSystem;

    private final String name;

    private final boolean isApp;

    public PhantomClassBuilder(TypeSystem typeSystem, PhantomClassSource source) {
        this.typeSystem = typeSystem;
        this.name = source.className();
        this.isApp = source.isApp();
    }

    @Override
    public void build(JClass jclass) {
        jclass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        // WARNING: all phantom classes that were 'implement'ed would not be set as interface.
        // Finer grained initialization should be taken.
        return EnumSet.noneOf(Modifier.class);
    }

    @Override
    public ClassType getClassType() {
        return typeSystem.getClassType(name);
    }

    @Override
    public JClass getSuperClass() {
        if (name.equals(ClassNames.OBJECT)) {
            return null;
        } else {
            // Object for phantom class. However, is it better to fake a "ILL" supertype?
            return typeSystem.objectType().getJClass();
        }
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return List.of();
    }

    @Override
    public JClass getOuterClass() {
        return null;
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return List.of();
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        return List.of();
    }

    @Override
    public AnnotationHolder getAnnotationHolder() {
        return AnnotationHolder.emptyHolder();
    }

    @Override
    public boolean isApplication() {
        return isApp;
    }

    @Override
    public boolean isPhantom() {
        return true;
    }

    @Nullable
    @Override
    public ClassGSignature getGSignature() {
        // for phantom class, generic signature is unavailable.
        return null;
    }
}
