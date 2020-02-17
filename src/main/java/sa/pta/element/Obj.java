package sa.pta.element;

public interface Obj  {

    enum Kind {
        NORMAL,
        STRING_CONSTANT,
        // array, class constants, ...
    }

    Type getType();

    Object getAllocationSite();

    Method getAllocationMethod();
}
