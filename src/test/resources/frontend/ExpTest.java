import java.lang.Exception;

class ExpTest {
    static int remainder(int dividend, int divisor)
    {
        try {
            return dividend % divisor;
        }
        catch (Exception e) {
            return 0;
        }
    }
}