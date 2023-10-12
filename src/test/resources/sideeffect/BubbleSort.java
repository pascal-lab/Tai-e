public class BubbleSort {

    public static void main(String[] args) {
        int[] array = new int[]{3, 4, 1, 2, 5};
        bubbleSort(array, 5);
    }

    public static void bubbleSort(int[] array, int len) {
        for (int i=0; i < len; i++) {
            for (int j=i+1; j < len; j++) {
                if (array[i] > array[j]) {
                    int tmp = array[i];
                    array[i] = array[j];
                    array[j] = tmp;
                }
            }
        }
    }

}