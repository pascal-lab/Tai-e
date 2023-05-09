package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.commons.JSRInlinerAdapter;

public record AsmMethodSource(JSRInlinerAdapter adapter, int classFileVersion) {
}
