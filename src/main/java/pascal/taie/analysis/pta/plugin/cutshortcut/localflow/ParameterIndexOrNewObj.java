package pascal.taie.analysis.pta.plugin.cutshortcut.localflow;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.cutshortcut.field.ParameterIndex;

import static pascal.taie.analysis.pta.plugin.cutshortcut.field.ParameterIndex.THISINDEX;

public record ParameterIndexOrNewObj(boolean isObj, ParameterIndex index, Obj obj) {
    public static ParameterIndexOrNewObj INDEX_THIS = new ParameterIndexOrNewObj(false, THISINDEX, null);

    @Override
    public String toString() {
        return isObj ? obj.toString() : index.toString();
    }
}

