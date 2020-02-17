package sa.pta.statement;

import sa.pta.element.Field;
import sa.pta.element.Variable;

/**
 * Represents an instance load: to = base.field.
 */
public class InstanceLoad implements Statement {

    private final Variable to;

    private final Variable base;

    private final Field field;

    public InstanceLoad(Variable to, Variable base, Field field) {
        this.to = to;
        this.base = base;
        this.field = field;
    }

    public Variable getTo() {
        return to;
    }

    public Variable getBase() {
        return base;
    }

    public Field getField() {
        return field;
    }
}
