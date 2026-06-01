public class Array {

    public static void main(String[] args) {
        int[] values = new int[4];
        values[0] = 1;
        values[1] = 4;
        values[2] = 9;
        values[3] = 16;

        check(values.length == 4);
        check(values[2] == 9);
        check(sum(values) == 30);

        Object[] objects = new Object[2];
        objects[0] = new Box(3);
        objects[1] = new Box(5);

        check(((Box) objects[0]).value == 3);
        check(((Box) objects[1]).value == 5);

        int[][] matrix = new int[2][2];
        matrix[0][0] = 1;
        matrix[0][1] = 2;
        matrix[1][0] = 3;
        matrix[1][1] = 4;

        check(matrix.length == 2);
        check(matrix[0].length == 2);
        check(matrix[1][1] == 4);

        System.out.println("OK");
    }

    private static int sum(int[] values) {
        int result = 0;
        for (int value : values) {
            result += value;
        }
        return result;
    }

    private static class Box {

        final int value;

        Box(int value) {
            this.value = value;
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
