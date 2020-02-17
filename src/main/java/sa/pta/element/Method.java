package sa.pta.element;

import java.util.List;
import java.util.Set;

public interface Method {

    boolean isInstance();

    boolean isStatic();

    Type getClassType();

    String getName();

    Variable getThis();

    List<Variable> getParameters();

    Set<Variable> getReturnVariables();
}
