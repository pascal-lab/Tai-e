package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.graph.flowgraph.InstanceNode;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.NewObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ObjectFlow implements TransInferStrategy {

    public static final String ID = "object-flow";

    private PointerAnalysisResult result;

    private OFGWithoutOther ofg;

    private Reachability<Node> reachability;

    @Override
    public void setContext(InfererContext context) {
        result = context.solver().getResult();
        ofg = new OFGWithoutOther(result.getObjectFlowGraph());
        reachability = new Reachability<>(ofg);
    }

    @Override
    public Set<InferredTransfer> apply(JMethod method, int index, Set<InferredTransfer> transfers) {
        if (transfers.isEmpty()) {
            return Set.of();
        }
        Var taintParam;
        if (index == InvokeUtils.BASE) {
            taintParam = method.getIR().getThis();
        } else {
            taintParam = method.getIR().getParam(index);
        }

        Set<Node> taintParamReach = reachability.reachableNodesFrom(ofg.getVarNode(taintParam));
        Set<Node> taintParamFieldReach = result.getPointsToSet(taintParam).stream()
                .filter(obj -> obj instanceof NewObj && obj.getType() instanceof ClassType)
                .map(obj -> ofg.getInstanceFieldNode(obj))
                .flatMap(Collection::stream)
                .map(reachability::reachableNodesFrom)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());

        Set<Integer> reachableIndex = Sets.newHybridSet();

        // TODO: add param index
        if (method.getReturnType() instanceof ReferenceType) {
            List<Var> returnVars = method.getIR().getReturnVars();
            if (returnVars.stream()
                    .noneMatch(retVar -> taintParamReach.contains(ofg.getVarNode(retVar)))) {
                Set<Node> resultFieldNodes = returnVars.stream()
                        .map(result::getPointsToSet)
                        .flatMap(Collection::stream)
                        .map(obj -> ofg.getInstanceFieldNode(obj))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toUnmodifiableSet());
                if(Sets.haveOverlap(taintParamReach, resultFieldNodes) ||
                        Sets.haveOverlap(taintParamFieldReach, resultFieldNodes))
                    reachableIndex.add(InvokeUtils.RESULT);
            }
        }
        // TODO: fix this
//        if (!method.isStatic()) {
//            if (canReach(method.getIR().getThis(), reachableNodes)) {
//                reachableIndex.add(InvokeUtils.BASE);
//            }
//        }

        return transfers.stream()
                .filter(tf -> reachableIndex.contains(tf.getTo().index()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean canReachTargetField(Var target, Set<Node> nodes) {
        Set<Obj> pts = result.getPointsToSet(target);
        return nodes.stream()
                .anyMatch(node -> node instanceof InstanceNode instanceNode
                        && pts.contains(instanceNode.getBase()));
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
