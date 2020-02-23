package sa.pta.analysis.context;

/**
 * Context with one element
 * @param <T>
 */
class OneContext<T> implements Context {

    private T element;

    OneContext(T element) {
        this.element = element;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OneContext<?> that = (OneContext<?>) o;
        return element.equals(that.element);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    @Override
    public String toString() {
        return "[" + element + "]";
    }
}
