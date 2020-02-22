package sa.pta.analysis.data;

import sa.pta.element.Field;

public class InstanceField extends AbstractPointer {

    private final CSObj base;

    private final Field field;

    InstanceField(CSObj base, Field field) {
        this.base = base;
        this.field = field;
    }

    public CSObj getBase() {
        return base;
    }

    public Field getField() {
        return field;
    }

    @Override
    public String toString() {
        return base + "." + field;
    }
}
