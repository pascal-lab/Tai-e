package pascal.taie.interp;

import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JClassObject {

    ClassType type;

    private final Map<String, JValue> staticFields;

    public JClassObject(ClassType type) {
        this.type = type;
        staticFields = new HashMap<>();
    }

    @Override
    public String toString() {
        return "JClassObj: [" + this.type + "]";
    }

    public JValue getStaticField(VM vm, FieldRef ref) {
        return staticFields.get(ref.getName());
    }

    public void setStaticField(VM vm, FieldRef ref, JValue value) {
        staticFields.put(ref.getName(), value);
    }

    public JValue invokeStatic(VM vm, JMethod method, List<JValue> values) {
        Map<Var, JValue> args = Maps.newMap();
        IR ir = method.getIR();
        for (int i = 0; i < method.getParamCount(); ++i) {
            args.put(ir.getParam(i), values.get(i));
        }
        Frame newFrame = Frame.mkNewFrame(args);
        return vm.execIR(ir, newFrame);
    }
}
