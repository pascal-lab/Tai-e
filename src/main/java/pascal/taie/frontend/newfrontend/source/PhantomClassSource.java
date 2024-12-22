package pascal.taie.frontend.newfrontend.source;

public class PhantomClassSource implements ClassSource {
    private final String className;

    private final boolean isApplication;

    public PhantomClassSource(String className, boolean isApplication) {
        this.className = className;
        this.isApplication = isApplication;
    }
    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean isApplication() {
        return isApplication;
    }
}
