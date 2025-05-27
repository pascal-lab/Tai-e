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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * We delegate some objects to JVM (see {@link Utils#isJVMClass} for what classes we delegate).
 * It's basically because we cannot handle native methods in {@link VM}.
 * For example,
 * <pre>
 * {@code
 * System.out.println("Hello World!");}
 * </pre>
 * Such method will eventually call a native method.
 * Thus, we need to delegate the object to JVM.
 * <p>
 * This class is used to represent a Java object in the Tai-e VM.
 * It contains the fields of the object and a reference to its class.
 * </p>
 * <p>
 * For method invocation and field access, we use reflection to access the fields and methods of the object.
 * </p>
 * <p>
 * This approaches has some limitations:
 * <ol>
 * <li>The class must be loaded in the running JVM of Tai-e, and we have not implemented a class loader
 * to load classes from {@link pascal.taie.World}. The class must be in the classpath of Tai-e.</li>
 * <li>From Java 9, we cannot access the private fields of a class in a module.
 * Thus, we cannot access
 * any private fields in {@link VM} for a JVM object.</li>
 * <li>This approach introduces complex transformations between JVM objects and Tai-e objects.</li>
 * </ol>
 * <p>
 * For limitations 1,
 * we may need to implement a class loader to load classes from {@link pascal.taie.World}.
 * <p>
 * For limitation 2,
 * we may need to use {@code --add-opens} to open the module to access the private fields.
 * <p>
 * For limitation 3, a {@link JObject} may be passed to a method of a JVM object.
 * For example
 * <pre>
 * {@code
 * A a = new A();
 * System.out.println(a);
 * }
 * </pre>
 * When {@code new A()} is a JObject, we can directly pass it to the {@code println} method.
 * However, JVM will call the {@code toString()} method of the object.
 * So we need to
 * hook the {@code toString()} method of the object to return the string representation.
 * See {@link pascal.taie.vm.JObject#toString()} for more details.
 * <p>
 * Though this case is a little complex, we can at least handle it.
 * However, we even
 * cannot handle some cases like:
 * <pre>
 * {@code
 * A a = new A(); // A is a JObject
 * B b = new B(); // B is a JVMObject
 * b.f(a);
 * }
 * </pre>
 * When we call the method {@code f} by reflection, an exception will be thrown by JVM as
 * the {@code a} in fact is a {@code JObject} instead of {@code A}. It will not type check.
 * <p>
 * Luckily, if the dividing of JVMObject and JObject is reasonable, this case will not occur
 * ({@code A} and {@code B} will be both JVMObject or both JObject).
 */
public class JVMObject extends JObject {

    /**
     * Real object in JVM.
     */
    private Object object;

    /**
     * Class object of the {@code this.object}.
     */
    private final JVMClassRep classObject;

    public JVMObject(JVMClassRep jClassObj, JMethod ctor, List<JValue> valueList) {
        super(null, jClassObj);
        classObject = jClassObj;
        init(ctor, valueList);
    }

    public JVMObject(JVMClassRep jClassObj, Object object) {
        super(null, jClassObj);
        this.classObject = jClassObj;
        this.object = object;
    }

    public JVMObject(JVMClassRep jClassObj) {
        super(null, jClassObj);
        this.classObject = jClassObj;
    }

    @Override
    public void setField(VM vm, FieldRef ref, JValue value) {
        try {
            Field f = Utils.toJVMField(ref.resolve());
            f.set(object, value.toJVMObj());
        } catch (IllegalAccessException e) {
            throw new VMException(e);
        }
    }

    @Override
    public JValue getField(VM vm, FieldRef ref) {
        Field field = Utils.toJVMField(ref.resolve());
        try {
            return Utils.fromJVMObject(vm, field.get(object), ref.getType());
        } catch (IllegalAccessException e) {
            throw new VMException(e);
        }
    }

    @Override
    public JValue invokeInstance(VM vm, JMethod method, List<JValue> args) {
        Method mtd = Utils.toJVMMethod(method);
        try {
            Object res = mtd.invoke(object, Utils.toJVMObjects(args, method.getParamTypes()));
            return Utils.fromJVMObject(vm, res, method.getReturnType());
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new VMException(e);
        }
    }

    /**
     * Modeling the constructor of the object.
     * @param ctor {@code <init>} method
     * @param args arguments of the constructor
     */
    public void init(JMethod ctor, List<JValue> args) {
        try {
            Class<?> klass = this.classObject.klass;
            Constructor<?> ctor1 = klass.getConstructor(Utils.toJVMTypeList(ctor.getParamTypes()));
            ctor1.setAccessible(true);
            this.object = ctor1.newInstance(Utils.toJVMObjects(args, ctor.getParamTypes()));
        } catch (NoSuchMethodException |
                 InstantiationException |
                 IllegalAccessException e) {
            throw new VMException(e);
        } catch (InvocationTargetException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public Object toJVMObj() {
        return object;
    }

    @Override
    public String toString() {
        return object.toString();
    }

}
