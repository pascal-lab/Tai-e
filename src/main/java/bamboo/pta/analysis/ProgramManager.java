package bamboo.pta.analysis;

import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;

import java.util.Collection;

public interface ProgramManager {

    Collection<Method> getEntryMethods();

    // -------------- type system ----------------
    boolean canAssign(Type from, Type to);

    Method resolveVirtualCall(Type recvType, Method method);

    Method resolveSpecialCall(CallSite callSite, Method container);
}
