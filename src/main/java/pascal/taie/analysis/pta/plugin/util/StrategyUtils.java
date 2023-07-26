package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StrategyUtils {

    private static final List<Class<? extends Type>> concernedTypes = List.of(ClassType.class);

    @Nullable
    public static CSVar getCSVar(CSManager csManager, CSCallSite csCallSite, int index) {
        Context context = csCallSite.getContext();
        if(csCallSite.getCallSite().isStatic() && index == InvokeUtils.BASE) {
            return null;
        }
        Var var = InvokeUtils.getVar(csCallSite.getCallSite(), index);
        if (var == null) {
            return null;
        }
        return csManager.getCSVar(context, var);
    }

    public static Set<Type> getTypes(Solver solver, CSVar csVar) {
        if(csVar == null) {
            return Set.of();
        }
        return solver.getPointsToSetOf(csVar)
                .objects()
                .map(csObj -> csObj.getObject().getType())
                .filter(type -> concernedTypes.contains(type.getClass()))
                .collect(Collectors.toSet());
    }
}
