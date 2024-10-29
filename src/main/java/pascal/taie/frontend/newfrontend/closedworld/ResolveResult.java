package pascal.taie.frontend.newfrontend.closedworld;

import pascal.taie.frontend.newfrontend.source.ClassSource;
import pascal.taie.util.collection.Pair;

import java.util.List;

public record ResolveResult(List<String> dependencies,
                            List<Pair<String, ClassSource>> resolvedSource) {
}
