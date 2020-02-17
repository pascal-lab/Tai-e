package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;

public abstract class AbstractCSElement implements CSElement {

    protected final Context context;

    AbstractCSElement(Context context) {
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }
}
