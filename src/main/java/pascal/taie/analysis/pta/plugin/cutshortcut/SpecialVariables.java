package pascal.taie.analysis.pta.plugin.cutshortcut;

import pascal.taie.analysis.pta.plugin.cutshortcut.field.ParameterIndex;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;

public class SpecialVariables {
    private static final Set<Var> definedVars = Sets.newSet();

    private static final Set<Var> virtualVars = Sets.newSet();

    private static final Map<Var, ParameterIndex> definedParameterIndexes = Maps.newMap();

    private static final Set<LoadField> relayedLoadFields = Sets.newSet();

    public static void setDefined(Var var) {
        definedVars.add(var);
    }

    public static boolean isDefined(Var var) {
        return definedVars.contains(var);
    }

    public static void setVirtualVar(Var var) {
        virtualVars.add(var);
    }

    public static boolean isVirtualVar(Var var) {
        return virtualVars.contains(var);
    }

    public static void setParameterIndex(Var param, ParameterIndex index) {
        definedParameterIndexes.put(param, index);
    }

    public static ParameterIndex getParameterIndex(Var param) {
        return definedParameterIndexes.get(param);
    }

    public static void disableRelay(LoadField loadField) {
        relayedLoadFields.add(loadField);
    }

    public static boolean isNonRelay(LoadField loadField) {
        return relayedLoadFields.contains(loadField);
    }
}
