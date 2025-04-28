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

package pascal.taie.vm;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;

import java.util.List;

/**
 * This class represents a class literal object in the Tai-e VM.
 * For example, {@code String.class} or {@code int[].class}.
 * Note we only use this class for {@link JObject}.
 * For {@link JVMObject}, we directly use the real {@code .class} value.
 * See {@code getClassLiteral(Class)} and {@code getClassLiteral(ClassLiteral)} of {@link VM} class
 * to see how it is created.
 * <p>
 * This class represents {@code .class} value, which is a real {@link JValue},
 * while {@link JClassRep} represents the class itself, contains the static fields and etc.
 * {@link JClassRep} is not a {@link JValue}, like real JVM class, its only used internal JVM.
 * Do not confuse them.
 * </p>
 */
public final class JClassLiteralObject extends JObject {

    final JClass klass;

    final int dimensions;

    public JClassLiteralObject(VM vm, JClass klass) {
        this(vm, klass, 0);
    }

    public JClassLiteralObject(VM vm, JClass klass, int dimensions) {
        super(vm, vm.loadClass(Utils.fromJVMClass(Class.class)));
        this.klass = klass;
        this.dimensions = dimensions;
    }


    @Override
    public void setField(VM vm, FieldRef ref, JValue value) {
        throw new VMException();
    }

    @Override
    public JValue getField(VM vm, FieldRef field) {
        throw new VMException();
    }

    @Override
    public JMethod getMethod(Subsignature subsignature) {
        return super.getMethod(subsignature);
    }

    @Override
    public JValue invokeInstance(VM vm, JMethod method, List<JValue> args) {
        if (method.getName().equals("getName")) {
            StringBuilder sb = new StringBuilder();
            if (dimensions > 0) {
                sb.append("[".repeat(dimensions));
                sb.append("L");
            }
            sb.append(klass.getName());
            return new JVMObject(vm.getSpecialClass(ClassNames.STRING), sb.toString());
        } else if (method.getName().equals("getEnclosingClass")) {
            JClass outer = this.klass.getOuterClass();
            // TODO: check if this behavior is correct
            if (outer == null) {
                return JNull.NULL;
            } else {
                return new JClassLiteralObject(vm, outer);
            }
        } else if (method.getName().equals("desiredAssertionStatus")) {
            return JPrimitive.getBoolean(true);
        }
        throw new VMException();
    }

    @Override
    public String toString() {
        return "Mock class object for " + klass;
    }

    @Override
    public Object toJVMObj() {
        throw new VMException();
    }
}
