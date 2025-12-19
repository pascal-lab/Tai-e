/**
 * Class with generic inner class.
 */
public class GenericInnerClass<T> {
    private T outerValue;

    public class Inner<U> {
        private T outerRef;
        private U innerValue;

        public T getOuterRef() {
            return outerRef;
        }

        public U getInnerValue() {
            return innerValue;
        }
    }

    public static class StaticInner<S> {
        private S value;

        public S getValue() {
            return value;
        }
    }
}
