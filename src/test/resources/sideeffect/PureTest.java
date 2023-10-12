public class PureTest {

    public static void main(String[] args) {
        int[] array = new int[]{3, 4, 1, 2, 5};
        countOnes(array, 5);
    }

    public static int countOnes(int[] array, int len) {
        int ret = 0;
        for (int i=0; i<len; i++) {
            ret = (array[i]==1)? ret+1 : ret;
        }
        return ret;
    }

}