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
final class JClassLiteralObject extends JObject {

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
        return switch (method.getName()) {
            case "getName" -> {
                StringBuilder sb = new StringBuilder();
                if (dimensions > 0) {
                    sb.append("[".repeat(dimensions));
                    sb.append("L");
                }
                sb.append(klass.getName());
                yield new JVMObject(vm.getSpecialClass(ClassNames.STRING), sb.toString());
            }
            case "getEnclosingClass" -> {
                // TODO: check if this behavior is correct
                JClass outer = this.klass.getOuterClass();
                yield outer == null ? JNull.NULL : new JClassLiteralObject(vm, outer);
            }
            case "desiredAssertionStatus" -> JPrimitive.getBoolean(true);
            default -> throw new VMException();
        };
    }

    @Override
    public Object toJVMObj() {
        throw new VMException();
    }

    @Override
    public String toString() {
        return "Mock class object for " + klass;
    }
}
