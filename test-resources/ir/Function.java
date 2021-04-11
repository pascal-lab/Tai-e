import java.util.function.BinaryOperator;

public class Function {

    public static void main(String[] args) {
        BinaryOperator<Long> functionAdd = (x, y) -> x + y;
        Long result = functionAdd.apply(1L, 2L);
        System.out.println(result);

        BinaryOperator<Long> function = (Long x, Long y) -> x + y;
        Long result2 = function.apply(1L, 2L);
        System.out.println(result2);
    }

}
