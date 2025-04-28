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

/**
 * A Java object.
 * <p>
 * This class is used to represent a Java object in the Tai-e VM.
 * It contains the fields of the object and a reference to its class.
 * </p>
 *
 * @see JVMObject
 * @see JClassLiteralObject
 */
public class JObject implements JValue {

    /**
     * The VM that this object belongs to.
     * It may look weird to have a back reference to the VM,
     * see {@link JObject#toString()} for the reason.
     */
    private final VM vm;

    /**
     * The class type of this object.
     */
    private final ClassType type;

    /**
     * Instance fields of the object.
     */
    private final Map<String, JValue> fields;

    /**
     * The class representation of this object.
     */
    private final JClassRep klass;

    /**
     * The super object of this object.
     */
    private JObject superObj;

    JObject(VM vm, JClassRep jClassObj) {
        this(vm, jClassObj, null);
    }

    JObject(VM vm, JClassRep jClassObj, JObject superObj) {
        this.vm = vm;
        this.klass = jClassObj;
        this.type = klass.type;
        fields = Maps.newMap();
        this.superObj = superObj;
    }

    private JObject(JObject obj) {
        // TODO: use correct, deep copy semantic
        this(obj.vm, obj.klass, obj.superObj == null ? null : new JObject(obj.superObj));
        fields.putAll(obj.fields);
    }

    /**
     * Set the field of the object.
     */
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

    /**
     * Get the field of the object.
     */
    public JValue getField(VM vm, FieldRef field) {
        String name = field.getName();
        if (field.resolve().getDeclaringClass().getType() != type) {
            // super field
            return superObj.getField(vm, field);
        }
        return fields.computeIfAbsent(name, n -> {
            JField f = type.getJClass().getDeclaredField(n);
            if (f == null) {
                throw new VMException();
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

    /**
     * Invoke a instance method.
     * <p>
     * We're modeling the {@code clone} and {@code getClass} methods here.
     * For {@code clone}, we create a new instance of the object.
     * For {@code getClass}, we return a {@link JClassLiteralObject} object.
     * </p>
     */
    public JValue invokeInstance(VM vm, JMethod method, List<JValue> args) {
        ClassType declType = method.getDeclaringClass().getType();
        if (Utils.isClone(method.getRef())) {
            return new JObject(this);
        } else if (Utils.isGetClass(method.getRef())) {
            return new JClassLiteralObject(vm, type.getJClass());
        }
        if (declType != type) {
            if (Utils.isJVMClass(declType) && method.getName().equals(MethodNames.INIT)) {
                // create an instance here
                assert superObj == null;
                superObj = new JVMObject((JVMClassRep) vm.loadClass(declType), method, args);
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
            Frame newFrame = Frame.makeNewFrame(argMap);
            return vm.execIR(newIr, newFrame);
        }
    }

    /**
     * Override the {@code toString} method.
     * See {@link JVMObject} for why we need to override this method.
     */
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
