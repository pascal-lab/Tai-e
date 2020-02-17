package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;

public abstract class AbstractCSElement implements CSElement {

    protected Context context;

    @Override
    public Context getContext() {
        return context;
    }
}
