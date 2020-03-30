package bamboo.pta.element;

import bamboo.pta.statement.Call;

import java.util.List;

public interface CallSite {

    boolean isVirtual();

    boolean isSpecial();

    boolean isStatic();

    /**
     *
     * @return the call statements containing this call site.
     */
    Call getCall();

    Method getMethod();

    Variable getReceiver();

    List<Variable> getArguments();

    Method getContainerMethod();
}
