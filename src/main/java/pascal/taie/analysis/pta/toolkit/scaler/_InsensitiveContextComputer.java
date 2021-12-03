package pascal.taie.analysis.pta.toolkit.scaler;

import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.language.classes.JMethod;

/**
 * Context-insensitive analysis can be seen as the analysis where
 * all contexts are merged as 1 context.
 */
class _InsensitiveContextComputer extends ContextComputer {

    _InsensitiveContextComputer(PointerAnalysisResultEx pta) {
        super(pta);
    }

    @Override
    String getVariantName() {
        return "context-insensitive";
    }

    @Override
    int computeContextNumberOf(JMethod method) {
        return 1;
    }
}
