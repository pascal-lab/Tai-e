package sa.pta.element;

import sa.pta.statement.Call;
import sa.pta.statement.InstanceLoad;
import sa.pta.statement.InstanceStore;

import java.util.Set;

public interface Variable {

    Type getType();

    Method getContainerMethod();

    String getName();

    /**
     *
     * @return set of call statements where this variable is the receiver.
     */
    Set<Call> getCalls();

    /**
     *
     * @return set of instance loads where this variable is the base.
     */
    Set<InstanceLoad> getLoads();

    /**
     *
     * @return set of instance stores where this variable is the base.
     */
    Set<InstanceStore> getStores();
}
