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

import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;

public class JObject implements JValue {

    private final VM vm;

    private final ClassType type;

    private final Map<String, JValue> fields;

    private final JClassObject klass;

    private JObject superObj;

    public JObject(VM vm, JClassObject jClassObj) {
        this(vm, jClassObj, null);
    }

    public JObject(VM vm, JClassObject jClassObj, JObject superObj) {
        this.vm = vm;
        this.klass = jClassObj;
        this.type = klass.type;
        fields = Maps.newMap();
        this.superObj = superObj;
    }

    public JObject(JObject obj) {
        // TODO: use correct, deep copy semantic
        this(obj.vm, obj.klass, obj.superObj == null ? null : new JObject(obj.superObj));
        fields.putAll(obj.fields);
    }

    public void setField(VM vm, FieldRef ref, JValue value) {
        if (ref.resolve().getDeclaringClass().getType() != type) {
            superObj.setField(vm, ref, value);
        } else {
            String name = ref.getName();
            fields.put(name, value);
        }
    }

    @Override
    public ClassType getType() {
        return type;
    }

    public JValue getField(VM vm, FieldRef field) {
        String name = field.getName();
        if (field.resolve().getDeclaringClass().getType() != type) {
            // super field
            return superObj.getField(vm, field);
        }
        return fields.computeIfAbsent(name, n -> {
            JField f = type.getJClass().getDeclaredField(n);
            if (f == null) {
                throw new InterpreterException();
            } else {
                if (f.getType() instanceof PrimitiveType t) {
                    return JPrimitive.getDefault(t);
                } else {
                    return null;
                }
            }
        });
    }

    public JMethod getMethod(Subsignature subsignature) {
        return type.getJClass().getDeclaredMethod(subsignature);
    }

    public JValue invokeInstance(VM vm, JMethod method, List<JValue> args) {
        ClassType declType = method.getDeclaringClass().getType();
        if (Utils.isClone(method.getRef())) {
            return new JObject(this);
        } else if (Utils.isGetClass(method.getRef())) {
            return new JMockClassObject(vm, type.getJClass());
        }
        if (declType != type) {
            if (Utils.isJVMClass(declType) && method.getName().equals(MethodNames.INIT)) {
                // create an instance here
                assert superObj == null;
                superObj = new JVMObject((JVMClassObject) vm.loadClass(declType), method, args);
                // must be null, void return value
                return null;
            } else {
                return superObj.invokeInstance(vm, method, args);
            }
        } else {
            Map<Var, JValue> argMap = Maps.newMap();
            IR newIr = method.getIR();
            Var jThis = newIr.getThis();
            argMap.put(jThis, this);
            for (int i = 0; i < method.getParamCount(); ++i) {
                argMap.put(newIr.getParam(i), args.get(i));
            }
            Frame newFrame = Frame.mkNewFrame(argMap);
            return vm.execIR(newIr, newFrame);
        }
    }

    @Override
    public String toString() {
        assert vm != null;
        JMethod mtd = type.getJClass().getDeclaredMethod("toString");
        assert mtd != null;
        JValue v = invokeInstance(vm, mtd, List.of());
        assert v instanceof JVMObject;
        Object o = v.toJVMObj();
        assert o instanceof String;
        return (String) o;
    }

    @Override
    public Object toJVMObj() {
        return this;
    }
}
