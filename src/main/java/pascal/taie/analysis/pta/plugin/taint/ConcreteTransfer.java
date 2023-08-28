package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

public class ConcreteTransfer extends TaintTransfer {
    public ConcreteTransfer(JMethod method, TransferPoint from, TransferPoint to, Type type) {
        super(method, from, to, type);
    }

}
