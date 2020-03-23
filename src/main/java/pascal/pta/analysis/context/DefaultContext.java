package pascal.pta.analysis.context;

public enum DefaultContext implements Context {
    INSTANCE,
    ;


    @Override
    public String toString() {
        return "[]";
    }
}
