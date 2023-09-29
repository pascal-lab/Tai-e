package pascal.taie.interp;

import pascal.taie.World;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;

import java.util.Arrays;
import java.util.List;

public class JArray implements JValue {
    private final JValue[] arr;

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

    public static JArray createArray(int count, Type baseType, int dims) {
        JArray arr = new JArray(new JValue[count], baseType, dims);
        if (dims == 1 && baseType instanceof PrimitiveType t) {
            for (int i = 0; i < count; ++i) {
                arr.setIdx(i, JPrimitive.getDefault(t));
            }
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
        Object[] arr = new Object[this.arr.length];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = this.arr[i].toJVMObj();
        }
        return arr;
    }

    @Override
    public Type getType() {
        return type;
    }

    public JArray(JArray another)  {
        this(another.arr.clone(), another.type.baseType(), another.type.dimensions());
    }

    public Class<?> mockGetClass() {
        return Utils.toJVMType(type);
    }
}
