package sa.pta.analysis.data;

import sa.pta.analysis.context.Context;
import sa.pta.element.Variable;

public class CSVariable extends AbstractPointer implements CSElement {

    private Context context;

    private Variable var;

    @Override
    public Context getContext() {
        return context;
    }

    public Variable getVariable() {
        return var;
    }
}
