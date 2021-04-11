import java.util.function.BinaryOperator;

public class MultiStatement {

    public static void main(String[] args){
        Runnable noArguments = () -> {
            System.out.println("Hello World");
            System.out.println("Hello World");
        };
        noArguments.run();
        int result  = getValue(3,5);
    }

    static int getValue(int input1, int input2){
        BinaryOperator<Integer> function = (Integer x, Integer y) -> x + y + 2;
        Integer result2 = function.apply(input1, input2);
        System.out.println(result2);
        return result2;
    }
}
