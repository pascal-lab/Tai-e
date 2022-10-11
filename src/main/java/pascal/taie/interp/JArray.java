package pascal.taie.interp;

import pascal.taie.language.type.ArrayType;

import java.lang.reflect.Array;
import java.util.List;

public class JArray implements JValue {
    private JValue[] arr;

    public JArray(JValue[] arr) {
        this.arr = arr;
    }

    public JValue getIdx(int idx) {
        return arr[idx];
    }

    public void setIdx(int idx, JValue v) {
        arr[idx] = v;
    }

    public static JArray createArray(JValue.ValueType t, int dim) {
        switch (t) {
            case JArray -> {
                return new JArray(new JArray[dim]);
            }
            case JLiteral -> {
                return new JArray(new JLiteral[dim]);
            }
            case JObject -> {
                return new JArray(new JObject[dim]);
            }
            default -> throw new IllegalStateException("");
        }
    }

    public static JArray createMultiArray(JValue.ValueType t, ArrayType at, List<Integer> dims, int dimIdx) {
        int maxSize = at.dimensions();
        if (maxSize - dimIdx <= 1) {
            return createArray(t, dims.get(dimIdx));
        }
        int now = dims.get(dimIdx);
        JArray arr = createArray(ValueType.JArray, now);
        for (int i = 0; i < now; ++i) {
            arr.setIdx(i, createMultiArray(t, at, dims, dimIdx + 1));
        }
        return arr;
    }

    @Override
    public String toString() {
        return "JArray [" + arr + "]";
    }

    @Override
    public Object toJavaObj() {
        Object[] arr = new JObject[this.arr.length];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = this.arr[i].toJavaObj();
        }
        return arr;
    }
}
