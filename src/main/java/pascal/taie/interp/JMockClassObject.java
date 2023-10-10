package pascal.taie.interp;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;

import java.util.List;

public final class JMockClassObject extends JObject {

    final JClass klass;

    final int dimensions;

    public JMockClassObject(VM vm, JClass klass) {
        this(vm, klass, 0);
    }

    public JMockClassObject(VM vm, JClass klass, int dimensions) {
        super(vm, vm.loadClass(Utils.fromJVMClass(Class.class)));
        this.klass = klass;
        this.dimensions = dimensions;
    }


    @Override
    public void setField(VM vm, FieldRef ref, JValue value) {
        throw new InterpreterException();
    }

    @Override
    public JValue getField(VM vm, FieldRef field) {
        throw new InterpreterException();
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
                return new JMockClassObject(vm, outer);
            }
        } else if (method.getName().equals("desiredAssertionStatus")) {
            return JPrimitive.getBoolean(true);
        }
        throw new InterpreterException();
    }

    @Override
    public String toString() {
        return "Mock class object for " + klass;
    }

    @Override
    public Object toJVMObj() {
        throw new InterpreterException();
    }
}
