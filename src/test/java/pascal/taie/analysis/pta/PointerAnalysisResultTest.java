package pascal.taie.analysis.pta;

import org.junit.jupiter.api.Test;
import pascal.taie.World;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.ArrayType;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PointerAnalysisResultTest {

    @Test
    void testNonFunctionalArrayIndexes() {
        Tests.testPTA(false, "basic", "ZeroLengthArray", "cs:1-obj-1h");
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        ClassHierarchy hierarchy = World.get().getClassHierarchy();

        int before = pta.getArrayIndexes().size();

        // Container.EMPTY = new A[0]
        JField field = hierarchy.getField("<Container: A[] EMPTY>");
        Set<Obj> pts = pta.getPointsToSet(field);
        assertTrue(pts.stream().noneMatch(Obj::isFunctional));

        // public Container() { data = EMPTY; }
        JMethod constructor = Objects.requireNonNull(hierarchy.getClass("Container"))
                .getDeclaredMethod(MethodNames.INIT);
        Collection<Var> vars = Objects.requireNonNull(constructor)
                .getIR().getVars().stream()
                .filter(v -> v.getType() instanceof ArrayType)
                .collect(Collectors.toUnmodifiableSet());

        // Ensure these operations does not trigger ArrayIndex creation
        pts.forEach(pta::getPointsToSet);
        // This relies on the fact that `index` variables are ignored by pta result
        vars.forEach(base -> pta.getPointsToSet(base, (Var) null));

        int after = pta.getArrayIndexes().size();
        assertEquals(before, after);
    }
}
