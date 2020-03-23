package pascal.pta.element;

public interface Field {

    boolean isInstance();

    boolean isStatic();

    /**
     *
     * @return The class type where this field is declared.
     */
    Type getClassType();

    String getName();

    Type getFieldType();
}
