package pascal.taie.project;

public interface ClassLike {
    String getClassName();

    default String getBinaryName() {
        return getInternalName().replace('/', '.');
    }

    String getInternalName();
}
