package sa.pta.analysis.data;

import sa.pta.element.Field;

public class InstanceField extends AbstractPointer {

    private CSObj base;

    private Field field;

    public CSObj getBase() {
        return base;
    }

    public Field getField() {
        return field;
    }
}
