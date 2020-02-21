package sa.pta.jimple;

import sa.pta.element.Field;
import soot.SootField;

class JimpleField implements Field {

    private SootField field;

    private JimpleType classType;

    private JimpleType fieldType;

    @Override
    public boolean isInstance() {
        return !field.isStatic();
    }

    @Override
    public boolean isStatic() {
        return field.isStatic();
    }

    @Override
    public JimpleType getClassType() {
        return classType;
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public JimpleType getFieldType() {
        return fieldType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleField that = (JimpleField) o;
        return field.equals(that.field);
    }

    @Override
    public int hashCode() {
        return field.hashCode();
    }
}
