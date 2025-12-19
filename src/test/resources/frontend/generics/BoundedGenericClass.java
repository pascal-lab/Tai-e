/**
 * Generic class with bounded type parameter.
 */
public class BoundedGenericClass<T extends Number> {
    private T number;

    public T getNumber() {
        return number;
    }

    public void setNumber(T number) {
        this.number = number;
    }
}
