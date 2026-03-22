package pascal.taie.analysis.pta.plugin.cutshortcut;

import pascal.taie.analysis.pta.plugin.reflection.ReflectiveCallEdge;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.ArrayType;
import pascal.taie.util.collection.Maps;

import java.util.Map;

import static pascal.taie.analysis.pta.core.solver.CutShortcutSolver.isConcerned;

public class ReflectiveEdgeProperty {
    // used for Cut-Shortcut
    public enum ReflectiveCallKind {
        NEW_INSTANCE, // r = c.newInstance(args), r is the baseVar
        METHOD_INVOKE, // r = m.invoke(obj, args): m is a instance of class method, and obj is a instance of m's declaring method,
        // args is a variable of array type which stores the argument of m in order
    }

    private static final Map<ReflectiveCallEdge, ReflectiveCallKind> reflectiveCallKindMap = Maps.newMap();

    private static final Map<ReflectiveCallEdge, Var> reflectiveCallVirtualArgMap = Maps.newMap();

    public static void setReflectiveKind(ReflectiveCallEdge reflectiveCallEdge, ReflectiveCallKind kind) {
        reflectiveCallKindMap.put(reflectiveCallEdge, kind);
    }

    public static ReflectiveCallKind getReflectiveKind(ReflectiveCallEdge reflectiveCallEdge) {
        return reflectiveCallKindMap.get(reflectiveCallEdge);
    }

    public static void setVirtualArg(ReflectiveCallEdge reflectiveCallEdge) {
        Var args = reflectiveCallEdge.getArgs();
        if (args != null && isConcerned(args)) {
            Var virtualArg = new Var(args.getMethod(), "VirtualArg", ((ArrayType) args.getType()).elementType(), -1);
            SpecialVariables.setVirtualVar(virtualArg);
            reflectiveCallVirtualArgMap.put(reflectiveCallEdge, virtualArg);
        }
    }

    public static Var getVirtualArg(ReflectiveCallEdge reflectiveCallEdge) {
        return reflectiveCallVirtualArgMap.getOrDefault(reflectiveCallEdge, null);
    }
}
