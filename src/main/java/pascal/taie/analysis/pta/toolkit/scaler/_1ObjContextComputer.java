package pascal.taie.analysis.pta.toolkit.scaler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.language.classes.JMethod;

class _1ObjContextComputer extends ContextComputer {

    private static final Logger logger = LogManager.getLogger(_1ObjContextComputer.class);

    _1ObjContextComputer(PointerAnalysisResultEx pta) {
        super(pta);
    }

    @Override
    String getVariantName() {
        return "1-obj";
    }

    @Override
    int computeContextNumberOf(JMethod method) {
        if (pta.getReceiverObjectsOf(method).isEmpty()) {
            logger.debug("Empty receiver: {}", method);
            return 1;
        }
        return pta.getReceiverObjectsOf(method).size();
    }
}
