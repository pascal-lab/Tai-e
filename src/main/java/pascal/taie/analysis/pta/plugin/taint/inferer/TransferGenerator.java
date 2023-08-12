package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import java.util.Set;
import java.util.stream.Collectors;

public class TransferGenerator {

    private static final int DEFAULT_WEIGHT = 1;

    private static final Set<Class<? extends Type>> concernedTypes = Set.of(ClassType.class);

    private final Solver solver;

    private final CSManager csManager;

    public TransferGenerator(Solver solver) {
        this.solver = solver;
        this.csManager = solver.getCSManager();
    }

    public Set<InferredTransfer> getTransfers(CSCallSite csCallSite, int from, int to) {
        if (getCSVar(csCallSite, from).getType() instanceof ReferenceType
                && getCSVar(csCallSite, to).getType() instanceof ReferenceType) {

            Set<Type> toTypes = getArgType(csCallSite, to);
            Set<JMethod> callees = solver.getCallGraph().getCalleesOf(csCallSite)
                    .stream()
                    .map(CSMethod::getMethod)
                    .collect(Collectors.toUnmodifiableSet());
            if(!toTypes.isEmpty() && !callees.isEmpty()) {
                TransferPoint fromPoint = new TransferPoint(TransferPoint.Kind.VAR, from, null);
                TransferPoint toPoint = new TransferPoint(TransferPoint.Kind.VAR, to, null);
                Set<InferredTransfer> result = Sets.newSet();
                for(JMethod callee : callees) {
                    for(Type toType : toTypes) {
                        result.add(new InferredTransfer(callee, fromPoint, toPoint, toType, DEFAULT_WEIGHT));
                    }
                }
                return result;
            }
        }
        return Set.of();
    }

    private CSVar getCSVar(CSCallSite csCallSite, int index) {
        Context context = csCallSite.getContext();
        Invoke callSite = csCallSite.getCallSite();
        return csManager.getCSVar(context, InvokeUtils.getVar(callSite, index));
    }

    private Set<Type> getArgType(CSCallSite csCallSite, int index) {
        CSVar csVar = getCSVar(csCallSite, index);
        return solver.getPointsToSetOf(csVar)
                .objects()
                .map(csObj -> csObj.getObject().getType())
                .filter(type -> concernedTypes.contains(type.getClass()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
