package sa.pta.analysis;

import sa.pta.element.Method;
import sa.pta.element.Type;

import java.util.Collection;

public interface ProgramManager {

    Collection<Method> getEntryMethods();

    // -------------- type system ----------------
    boolean isAssignable(Type from, Type to);
}
