import java.util.List;

/**
 * Class demonstrating wildcard usage.
 */
public class WildcardClass {
    private List<?> unbounded;
    private List<? extends Number> upperBounded;
    private List<? super Integer> lowerBounded;

    public void processUnbounded(List<?> list) {
    }

    public void processUpperBounded(List<? extends Number> list) {
    }

    public void processLowerBounded(List<? super Integer> list) {
    }
}
