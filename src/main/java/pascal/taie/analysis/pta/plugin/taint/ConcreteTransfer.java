package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

class ConcreteTransfer extends TaintTransfer {
    ConcreteTransfer(JMethod method, TransferPoint from, TransferPoint to, Type type) {
        super(method, from, to, type);
    }

}
