package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InferredTransfer that = (InferredTransfer) o;
        return weight == that.weight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), weight);
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
