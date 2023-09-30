package pascal.taie.interp;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class JVMObject extends JObject {

    private Object object;

    private final JVMClassObject classObject;

    public JVMObject(JVMClassObject jClassObj, JMethod ctor, List<JValue> valueList) {
        super(null, jClassObj);
        classObject = jClassObj;
        init(ctor, valueList);
    }

    public JVMObject(JVMClassObject jClassObj, Object object) {
        super(null, jClassObj);
        this.classObject = jClassObj;
        this.object = object;
    }

    public JVMObject(JVMClassObject jClassObj) {
        super(null, jClassObj);
        this.classObject = jClassObj;
    }

    @Override
    public void setField(VM vm, FieldRef ref, JValue value) {
        try {
            Field f = Utils.toJVMField(ref.resolve());
            f.set(object, value.toJVMObj());
        } catch (IllegalAccessException e) {
            throw new InterpreterException(e);
        }
    }

    @Override
    public JValue getField(VM vm, FieldRef ref) {
        Field field = Utils.toJVMField(ref.resolve());
        try {
            return Utils.fromJVMObject(vm, field.get(object), ref.getType());
        } catch (IllegalAccessException e) {
            throw new InterpreterException(e);
        }
    }

    @Override
    public JValue invokeInstance(VM vm, JMethod method, List<JValue> args) {
        Method mtd = Utils.toJVMMethod(method);
        try {
            Object res = mtd.invoke(object, Utils.toJVMObjects(args, method.getParamTypes()));
            return Utils.fromJVMObject(vm, res, method.getReturnType());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InterpreterException(e);
        }
    }

    public void init(JMethod ctor, List<JValue> args) {
        try {
            Class<?> klass = this.classObject.klass;
            Constructor<?> ctor1 = klass.getConstructor(Utils.toJVMTypeList(ctor.getParamTypes()));
            ctor1.setAccessible(true);
            this.object = ctor1.newInstance(Utils.toJVMObjects(args, ctor.getParamTypes()));
        } catch (NoSuchMethodException |
                 InstantiationException |
                 IllegalAccessException e) {
            throw new InterpreterException(e);
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
