import java.util.List;

class Lambda {
    public static void main(String[] args) {
        System.out.println(List.of(1, 2, 3)
                .stream()
                .map(i -> i * 2)
                .reduce(0, Lambda::sum));
    }

    public static int sum(int i, int j) {
        return i + j;
    }
}
