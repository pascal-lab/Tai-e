package sa.pta.statement;

import sa.pta.element.Variable;

/**
 * Represents a local assignment: to = from;
 */
public class Assign implements Statement {

    private final Variable from;

    private final Variable to;

    public Assign(Variable from, Variable to) {
        this.from = from;
        this.to = to;
    }

    public Variable getFrom() {
        return from;
    }

    public Variable getTo() {
        return to;
    }
}
