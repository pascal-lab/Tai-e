package sa.pta.element;

public interface Variable {

    Type getType();

    Method getContainingMethod();

    String getName();
}
