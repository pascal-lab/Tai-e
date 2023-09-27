package pascal.taie.interp;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class JVMObject extends JObject {

    private final Object object;

    private final JVMClassObject classObject;

    public JVMObject(JVMClassObject jClassObj, JMethod ctor, List<JValue> valueList) {
        super(jClassObj);
        classObject = jClassObj;
        try {
            Class<?> klass = jClassObj.klass;
            Constructor<?> ctor1 = klass.getConstructor(Utils.toJVMTypeList(ctor.getParamTypes()));
            ctor1.setAccessible(true);
            this.object = ctor1.newInstance(Utils.toJVMObjects(valueList));
        } catch (NoSuchMethodException |
                 InvocationTargetException |
                 InstantiationException |
                 IllegalAccessException e) {
            throw new InterpreterException(e);
        }
    }

    public JVMObject(JVMClassObject jClassObj, Object object) {
        super(jClassObj);
        this.classObject = jClassObj;
        this.object = object;
    }

    @Override
    public void setField(VM vm, FieldRef ref, JValue value) {
        Class<?> klass = Utils.toJVMType(ref.getDeclaringClass().getType());
        try {
            Field f = klass.getField(ref.getName());
            f.setAccessible(true);
            f.set(object, value.toJVMObj());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JValue getField(VM vm, FieldRef ref) {
        Class<?> klass = Utils.toJVMType(ref.getDeclaringClass().getType());
        try {
            Field f = klass.getField(ref.getName());
            f.setAccessible(true);
            return Utils.fromJVMObject(vm, f.get(object), ref.getType());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JValue invokeInstance(VM vm, JMethod method, List<JValue> args) {
        Method mtd = Utils.toJVMMethod(method);
        try {
            Object res = mtd.invoke(object, Utils.toJVMObjects(args));
            return Utils.fromJVMObject(vm, res, method.getReturnType());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InterpreterException(e);
        }
    }

    @Override
    public Object toJVMObj() {
        return object;
    }
}
