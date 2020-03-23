package pascal.pta.analysis;

import pascal.pta.element.CallSite;
import pascal.pta.element.Method;
import pascal.pta.element.Type;

import java.util.Collection;

public interface ProgramManager {

    Collection<Method> getEntryMethods();

    // -------------- type system ----------------
    boolean canAssign(Type from, Type to);

    Method resolveVirtualCall(Type recvType, Method method);

    Method resolveSpecialCall(CallSite callSite, Method container);
}
