package pascal.taie.interp;

import pascal.taie.World;
import pascal.taie.language.type.ArrayType;
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
        return new JArray(new JValue[count], baseType, dims);
    }

    public static JArray createMultiArray(ArrayType at, List<Integer> dims, int dimIdx) {
        int maxSize = at.dimensions();
        if (maxSize - dimIdx <= 1) {
            return createArray(dims.get(dimIdx), at.baseType(), 1);
        }
        int now = dims.get(dimIdx);
        JArray arr = createArray(now, at.baseType(), at.dimensions());
        for (int i = 0; i < now; ++i) {
            arr.setIdx(i, createMultiArray((ArrayType) at.elementType(), dims, dimIdx + 1));
        }
        return arr;
    }

    @Override
    public String toString() {
        return "JArray " + Arrays.toString(arr);
    }

    @Override
    public Object toJVMObj() {
        Object[] arr = new JObject[this.arr.length];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = this.arr[i].toJVMObj();
        }
        return arr;
    }

    @Override
    public Type getType() {
        return type;
    }
}
