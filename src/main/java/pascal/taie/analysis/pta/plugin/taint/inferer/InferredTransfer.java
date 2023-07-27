package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

public class InferredTransfer extends TaintTransfer implements Comparable<InferredTransfer> {

    private int weight;

    public InferredTransfer(JMethod method,
                            TransferPoint from,
                            TransferPoint to,
                            Type type,
                            int weight) {
        super(method, from, to, type);
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "InferredTransfer{ " + super.toString() + " }";
    }

    @Override
    public int compareTo(InferredTransfer other) {
        return Integer.compare(weight, other.weight);
    }
}
