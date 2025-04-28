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
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * This class wraps a JVM class.
 *
 * @see JVMObject
 * @see JClassRep
 */
public class JVMClassRep extends JClassRep {
    final Class<?> klass;

    public JVMClassRep(ClassType type) {
        super(type);
        try {
            klass = Class.forName(type.getName());
        } catch (ClassNotFoundException e) {
            throw new VMException(e);
        }
    }

    public JVMClassRep(ClassType ct, Class<?> klass) {
        super(ct);
        this.klass = klass;
    }

    @Override
    public JValue invokeStatic(VM vm, JMethod method, List<JValue> args)  {
        try {
            Method mtd = Utils.toJVMMethod(method);
            Object[] arr = Utils.toJVMObjects(args, method.getParamTypes());
            Object v = mtd.invoke(null, arr);
            JValue res = Utils.fromJVMObject(vm, v, method.getReturnType());
            for (int i = 0; i < arr.length; ++i) {
                JValue vi = args.get(i);
                Object oi = arr[i];
                if (vi instanceof JArray arri) {
                    arri.update((JArray) Utils.fromJVMObject(vm, oi, arri.getType()));
                }
            }
            return res;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new VMException(e);
        } catch (InvocationTargetException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public JValue getStaticField(VM vm, FieldRef ref) {
        try {
            Field field = klass.getDeclaredField(ref.getName());
            field.setAccessible(true);
            return Utils.fromJVMObject(vm, field.get(null), ref.getType());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new VMException(e);
        }
    }

    @Override
    public void setStaticField(VM vm, FieldRef ref, JValue value) {
        try {
            Field field = klass.getDeclaredField(ref.getName());
            field.setAccessible(true);
            field.set(null, value.toJVMObj());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new VMException(e);
        }
    }
}
