package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;
import sa.pta.element.Variable;

public class CSVariable extends AbstractPointer implements CSElement {

    private final Context context;

    private final Variable var;

    CSVariable(Context context, Variable var) {
        this.context = context;
        this.var = var;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public Variable getVariable() {
        return var;
    }
}
