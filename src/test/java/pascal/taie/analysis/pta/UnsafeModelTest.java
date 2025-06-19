package pascal.taie.analysis.pta;

import org.junit.jupiter.api.Test;
import pascal.taie.World;
import pascal.taie.analysis.Tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnsafeModelTest {
    @Test
    void testNonExistStaticField() {
        Tests.testPTA(false, "misc", "NonExistStaticField");
        PointerAnalysisResult result = World.get().getResult(PointerAnalysis.ID);
        assertTrue(result.getInstanceFields().stream()
                .noneMatch(x -> x.getField().isStatic()));
    }
}
