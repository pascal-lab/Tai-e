package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.type.TypeSystem;

/**
 * Base class for the objects that holds a {@link Solver}.
 * It also stores various objects obtained from the {@link Solver},
 * so that its subclasses can directly access these objects.
 */
public abstract class SolverHolder {

    protected final Solver solver;

    protected final ClassHierarchy hierarchy;

    protected final TypeSystem typeSystem;

    protected final ContextSelector selector;

    protected final Context emptyContext;

    protected final CSManager csManager;

    protected final HeapModel heapModel;

    protected SolverHolder(Solver solver) {
        this.solver = solver;
        hierarchy = solver.getHierarchy();
        typeSystem = solver.getTypeSystem();
        selector = solver.getContextSelector();
        emptyContext = selector.getEmptyContext();
        csManager = solver.getCSManager();
        heapModel = solver.getHeapModel();
    }
}
