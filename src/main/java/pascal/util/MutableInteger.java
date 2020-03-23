package pascal.util;

public class MutableInteger {

    private int value;

    public MutableInteger(int value) {
        this.value = value;
    }

    public void set(int value) {
        this.value = value;
    }

    public int get() {
        return value;
    }

    /**
     * Increase the value by one and then return it.
     * @return the increased value.
     */
    public int increase() {
        return ++value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableInteger that = (MutableInteger) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
