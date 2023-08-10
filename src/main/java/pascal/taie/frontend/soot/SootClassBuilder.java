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

package pascal.taie.frontend.soot;

import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.generics.GSignatures;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Lists;
import soot.SootClass;
import soot.tagkit.SignatureTag;
import soot.tagkit.Tag;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

import static pascal.taie.language.classes.ClassNames.OBJECT;

class SootClassBuilder implements JClassBuilder {

    private final Converter converter;

    private final SootClass sootClass;

    SootClassBuilder(Converter converter, SootClass sootClass) {
        this.converter = converter;
        this.sootClass = sootClass;
    }

    @Override
    public void build(JClass jclass) {
        jclass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Modifiers.convert(sootClass.getModifiers());
    }

    @Override
    public String getSimpleName() {
        return sootClass.getShortName();
    }

    @Override
    public ClassType getClassType() {
        return (ClassType) converter.convertType(sootClass.getType());
    }

    @Override
    public JClass getSuperClass() {
        if (sootClass.getName().equals(OBJECT)) {
            return null;
        } else {
            return converter.convertClass(sootClass.getSuperclass());
        }
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return Lists.map(sootClass.getInterfaces(), converter::convertClass);
    }

    @Override
    public JClass getOuterClass() {
        return sootClass.hasOuterClass() ?
                converter.convertClass(sootClass.getOuterClass()) :
                null;
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return Lists.map(sootClass.getFields(), converter::convertField);
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        return Lists.map(sootClass.getMethods(), converter::convertMethod);
    }

    @Override
    public AnnotationHolder getAnnotationHolder() {
        return Converter.convertAnnotations(sootClass);
    }

    @Override
    public boolean isApplication() {
        return sootClass.isApplicationClass();
    }

    @Override
    public boolean isPhantom() {
        return sootClass.isPhantom();
    }

    @Nullable
    @Override
    public ClassGSignature getGSignature() {
        Tag tag = sootClass.getTag("SignatureTag");
        if (tag instanceof SignatureTag signatureTag) {
            return GSignatures.toClassSig(sootClass.isInterface(),
                    signatureTag.getSignature());
        }
        return null;
    }
}
