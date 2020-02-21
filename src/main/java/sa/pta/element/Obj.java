package sa.pta.element;

public interface Obj  {

    enum Kind {
        NORMAL,
        STRING_CONSTANT,
        // array, class constants, ...
    }

    Type getType();

    Object getAllocationSite();

    /**
     *
     * @return the method containing the allocation site of this object.
     * Returns null for Some special objects, e.g., string constants,
     * which do not have such method.
     */
    Method getContainerMethod();
}
