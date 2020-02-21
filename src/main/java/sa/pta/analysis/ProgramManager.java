package sa.pta.analysis;

import sa.pta.element.CallSite;
import sa.pta.element.Method;
import sa.pta.element.Type;

import java.util.Collection;

public interface ProgramManager {

    Collection<Method> getEntryMethods();

    // -------------- type system ----------------
    boolean canAssign(Type from, Type to);

    Method resolveVirtualCall(Type recvType, CallSite callSite);

    Method resolveSpecialCall(Type recvType, CallSite callSite);
}
