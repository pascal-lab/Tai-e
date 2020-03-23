package pascal.pta.analysis.solver;

import pascal.pta.analysis.data.Pointer;
import pascal.pta.element.Type;

import java.util.Objects;

class PointerFlowEdge {

    enum Kind {
        LOCAL_ASSIGN,
        CAST,

        THIS_PASSING,
        PARAMETER_PASSING,
        RETURN,

        INSTANCE_LOAD,
        INSTANCE_STORE,
    }

    private final Kind kind;

    private final Pointer from;

    private final Pointer to;

    /**
     * Type for cast assignment.
     */
    private final Type type;

    public PointerFlowEdge(Kind kind, Pointer from, Pointer to, Type type) {
        this.kind = kind;
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public Kind getKind() {
        return kind;
    }

    public Pointer getFrom() {
        return from;
    }

    public Pointer getTo() {
        return to;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointerFlowEdge that = (PointerFlowEdge) o;
        return kind == that.kind &&
                from.equals(that.from) &&
                to.equals(that.to) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, from, to, type);
    }

    @Override
    public String toString() {
        return "[" + kind + "]" + from + " -> " + to;
    }
}
