package pascal.pta.analysis.data;

import pascal.pta.analysis.context.Context;

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
