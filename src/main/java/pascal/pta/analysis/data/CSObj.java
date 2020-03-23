package pascal.pta.analysis.data;

import pascal.pta.analysis.context.Context;
import pascal.pta.element.Obj;

public class CSObj extends AbstractCSElement {

    private final Obj obj;

    CSObj(Context context, Obj obj) {
        super(context);
        this.obj = obj;
    }

    public Obj getObject() {
        return obj;
    }

    @Override
    public String toString() {
        return context + ":" + obj;
    }
}
