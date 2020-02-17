package sa.pta.element;

import java.util.List;

public interface CallSite {

    boolean isVirtual();

    boolean isSpecial();

    boolean isStatic();

    Method getMethod();

    Variable getReceiver();

    List<Variable> getArguments();

    Method getContainingMethod();
}
