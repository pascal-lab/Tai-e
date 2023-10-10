package pascal.taie.interp;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class JVMClassObject extends JClassObject {
    final Class<?> klass;

    public JVMClassObject(ClassType type) {
        super(type);
        try {
            klass = Class.forName(type.getName());
        } catch (ClassNotFoundException e) {
            throw new InterpreterException(e);
        }
    }

    public JVMClassObject(ClassType ct, Class<?> klass) {
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
            throw new InterpreterException(e);
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
            throw new InterpreterException(e);
        }
    }

    @Override
    public void setStaticField(VM vm, FieldRef ref, JValue value) {
        try {
            Field field = klass.getDeclaredField(ref.getName());
            field.setAccessible(true);
            field.set(null, value.toJVMObj());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new InterpreterException(e);
        }
    }
}
