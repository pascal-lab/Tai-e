package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.Collection;
import java.util.Set;

class TypeTransferStrategy implements TransInferStrategy {

    private Solver solver;

    private TaintConfig config;

    private TypeTransferGraph typeTransferGraph;

    @Override
    public void setContext(InfererContext context) {
        this.solver = context.solver();
        this.config = context.config();
        this.typeTransferGraph = new TypeTransferGraph();

        ClassHierarchy classHierarchy = solver.getHierarchy();
        classHierarchy.allClasses()
                .forEach(jClass -> {
                    Type type = jClass.getType();
                    Collection<JClass> subClasses = classHierarchy.getAllSubclassesOf(jClass);
                    subClasses.forEach(subClass -> typeTransferGraph.addEdge(subClass.getType(), type));
                });

        // TODO: collect information from CSCallsite
    }

    @Override
    public Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers) {
        // TODO
        return null;
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
