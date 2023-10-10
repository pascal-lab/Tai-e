package pascal.taie.interp;

import pascal.taie.World;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class JArray implements JValue {
    private JValue[] arr;

    private final ArrayType type;

    public JArray(JValue[] arr, Type baseType, int dims) {
        this.type = World.get().getTypeSystem().getArrayType(baseType, dims);
        this.arr = arr;
    }

    public JValue getIdx(int idx) {
        return arr[idx];
    }

    public void setIdx(int idx, JValue v) {
        arr[idx] = v;
    }

    public int length() {
        return arr.length;
    }

    public static JArray createArray(int count, Type baseType, int dims) {
        JArray arr = new JArray(new JValue[count], baseType, dims);
        JValue ele = dims == 1 && baseType instanceof PrimitiveType t ?
                JPrimitive.getDefault(t) : JNull.NULL;
        for (int i = 0; i < count; ++i) {
            arr.setIdx(i, ele);
        }
        return arr;
    }

    public static JArray createMultiArray(ArrayType at, List<Integer> counts, int countIdx) {
        int now = counts.get(countIdx);
        JArray arr = createArray(now, at.baseType(), at.dimensions());
        if (countIdx + 1 < counts.size()) {
            for (int i = 0; i < now; ++i) {
                Type eleType = at.elementType();
                JValue ele = eleType instanceof ArrayType eleAt ?
                        createMultiArray(eleAt, counts, countIdx + 1) :
                        createArray(counts.get(countIdx + 1), eleType, 1);
                arr.setIdx(i, ele);
            }
        }
        return arr;
    }

    @Override
    public String toString() {
        return "JArray " + Arrays.toString(arr);
    }

    @Override
    public Object toJVMObj() {
        Class<?> klass = Utils.toJVMType(type);
        Object res = Array.newInstance(klass.getComponentType(), this.arr.length);
        for (int i = 0; i < arr.length; ++i) {
            // TODO: warning when try to convert from non-jvm objects
            Array.set(res, i, Utils.typedToJVMObj(arr[i], type.elementType()));
        }
        return res;
    }

    @Override
    public Type getType() {
        return type;
    }

    public JArray(JArray another)  {
        this(another.arr.clone(), another.type.baseType(), another.type.dimensions());
    }

    public void update(JArray another) {
        this.arr = another.arr;
    }

    public JObject mockGetClass(VM vm) {
        if (Utils.isJVMClass(type)) {
            return vm.getClassLiteral(Utils.toJVMType(type));
        } else {
            assert type.elementType() instanceof ClassType;
            return new JMockClassObject(vm, ((ClassType) type.baseType()).getJClass());
        }
    }
}
