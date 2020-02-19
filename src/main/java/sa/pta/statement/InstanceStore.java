package sa.pta.statement;

import sa.pta.element.Field;
import sa.pta.element.Variable;

/**
 * Represents an instance store: base.field = from.
 */
public class InstanceStore implements Statement {

    private final Variable base;

    private final Field field;

    private final Variable from;

    public InstanceStore(Variable base, Field field, Variable from) {
        this.base = base;
        this.field = field;
        this.from = from;
    }

    public Variable getBase() {
        return base;
    }

    public Field getField() {
        return field;
    }

    public Variable getFrom() {
        return from;
    }

    @Override
    public Kind getKind() {
        return Kind.INSTANCE_STORE;
    }
}
