package pascal.taie.frontend.newfrontend.source;

public record PhantomClassSource(String className, boolean isApp)
        implements ClassSource {

    @Override
    public String getClassName() {
        return className;
    }
}
