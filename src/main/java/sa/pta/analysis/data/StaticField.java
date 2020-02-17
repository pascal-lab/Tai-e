package sa.pta.analysis.data;

import sa.pta.element.Field;

public class StaticField extends AbstractPointer {

    private final Field field;

    StaticField(Field field) {
        this.field = field;
    }

    public Field getField() {
        return field;
    }
}
