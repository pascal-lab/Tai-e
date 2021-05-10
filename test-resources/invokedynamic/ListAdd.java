import java.util.ArrayList;
import java.util.List;

public class ListAdd {

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; i++){
            list.add(i);
        }
        List<Integer> list1 = new ArrayList<>();
        list.forEach(list1::add);
    }
}
