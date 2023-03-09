package pascal.taie.language.type;

public enum BottomType implements Type{
    BOTTOM_TYPE;

    @Override
    public String getName() {
        return "bottom_type";
    }

    @Override
    public String toString() {
        return getName();
    }
}
