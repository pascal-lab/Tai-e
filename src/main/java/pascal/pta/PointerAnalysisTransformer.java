package pascal.pta;

import pascal.pta.analysis.context.ContextInsensitiveSelector;
import pascal.pta.analysis.data.CSMethod;
import pascal.pta.analysis.data.CSObj;
import pascal.pta.analysis.data.CSVariable;
import pascal.pta.analysis.data.HashDataManager;
import pascal.pta.analysis.data.InstanceField;
import pascal.pta.analysis.data.Pointer;
import pascal.pta.analysis.heap.AllocationSiteBasedModel;
import pascal.pta.analysis.solver.PointerAnalysis;
import pascal.pta.analysis.solver.PointerAnalysisImpl;
import pascal.pta.element.Obj;
import pascal.pta.jimple.JimpleProgramManager;
import pascal.pta.set.HybridPointsToSet;
import pascal.pta.set.PointsToSetFactory;
import soot.SceneTransformer;
import soot.jimple.AssignStmt;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

public class PointerAnalysisTransformer extends SceneTransformer {

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        PointerAnalysis pta = new PointerAnalysisImpl();
        pta.setProgramManager(new JimpleProgramManager());
        pta.setContextSelector(new ContextInsensitiveSelector());
//        pta.setContextSelector(new OneObjectSelector());
//        pta.setContextSelector(new OneCallSelector());
        pta.setHeapModel(new AllocationSiteBasedModel());
        PointsToSetFactory setFactory = new HybridPointsToSet.Factory();
        pta.setDataManager(new HashDataManager(setFactory));
        pta.setPointsToSetFactory(setFactory);
        pta.solve();
        System.out.println("Reachable methods:");
        pta.getCallGraph().getReachableMethods()
                .stream()
                .sorted(Comparator.comparing(CSMethod::toString))
                .forEach(System.out::println);
        System.out.println("Call graph edges:");
        pta.getCallGraph().getAllEdges().forEach(System.out::println);
        printVariables(pta.getVariables());
        printInstanceFields(pta.getInstanceFields());
    }

    private void printVariables(Stream<CSVariable> vars) {
        System.out.println("Points-to sets of all variables:");
        vars.sorted(Comparator.comparing(p -> p.getVariable().toString()))
                .forEach(this::printPointsToSet);
    }

    private void printInstanceFields(Stream<InstanceField> fields) {
        System.out.println("Points-to sets of all instance fields:");
        fields.sorted(Comparator.comparing(f -> f.getBase().toString()))
                .forEach(this::printPointsToSet);
    }

    private void printPointsToSet(Pointer pointer) {
        String ptr;
        if (pointer instanceof InstanceField) {
            InstanceField f = (InstanceField) pointer;
            ptr = objToString(f.getBase().getObject()) + "." + f.getField();
        } else {
            ptr = pointer.toString();
        }
        System.out.print(ptr + " -> {");
        pointer.getPointsToSet().stream()
                .sorted(Comparator.comparing(CSObj::toString))
                .forEach(o -> System.out.print(objToString(o.getObject()) + ","));
        System.out.println("}");
    }

    private String objToString(Obj obj) {
        StringBuilder sb = new StringBuilder();
        if (obj.getContainerMethod() != null) {
            sb.append(obj.getContainerMethod()).append('/');
        }
        Object allocation = obj.getAllocationSite();
        if (allocation instanceof AssignStmt) {
            AssignStmt alloc = (AssignStmt) allocation;
            sb.append(alloc.getRightOp())
                    .append('/')
                    .append(alloc.getJavaSourceStartLineNumber());
        } else {
            sb.append(allocation);
        }
        return sb.toString();
    }
}
