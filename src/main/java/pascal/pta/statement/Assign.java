package pascal.pta.statement;

import pascal.pta.element.Variable;

/**
 * Represents a local assignment: to = from;
 */
public class Assign implements Statement {

    private final Variable to;

    private final Variable from;

    public Assign(Variable to, Variable from) {
        this.to = to;
        this.from = from;
    }

    public Variable getTo() {
        return to;
    }

    public Variable getFrom() {
        return from;
    }

    @Override
    public Kind getKind() {
        return Kind.ASSIGN;
    }

    @Override
    public String toString() {
        return from + " = " + to;
    }
}
