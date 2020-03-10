package sa.pta.analysis.data;

public class ArrayField extends AbstractPointer {

    private final CSObj array;

    ArrayField(CSObj array) {
        this.array = array;
    }

    public CSObj getArray() {
        return array;
    }

    @Override
    public String toString() {
        return array + "[*]";
    }
}
