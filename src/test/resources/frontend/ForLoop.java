import java.util.ArrayList;
import java.util.List;

class ForLoop {
    public int f() {
        int temp = 10;
        for (int i = 0; i < 100; i = i + 1) {
            temp = temp * i;
            if (temp < 0) {
                break;
            }
        }
        return temp;
    }

    public int g() {
        List<Integer> l = new ArrayList<Integer>();
        for (float i : l) {
            int j = (int) i;
        }

        int[] t = new int[] { 1, 2, 3 };
        for (int i : t) {
            int j = i;
        }
        return 20;
    }
}