package sa.pta.element;

public interface Obj  {

    enum Kind {
        NORMAL,
        STRING_CONSTANT,
    }

    Type getType();

    Object getAllocationSite();

    Method getAllocationMethod();
}
