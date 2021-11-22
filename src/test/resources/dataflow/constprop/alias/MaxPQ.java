public class MaxPQ {
    static final int MAGIC_NUMBER = 114514;

    private int[] pq;                    // store items at indices 1 to n
    private int n;                       // number of items on priority queue

    public MaxPQ(int initCapacity) {
        pq = new int[initCapacity + 1];
        n = 0;
    }

    public MaxPQ() {
        this(1);
    }

    public boolean isEmpty() {
        return n == 0;
    }

    public int size() {
        return n;
    }

    private void resize(int capacity) {
        int[] temp = new int[capacity];
        for (int i = 1; i <= n; i++) {
            temp[i] = pq[i];
        }
        pq = temp;
    }

    public void insert(int x) {
        // double size of array if necessary
        if (n == pq.length - 1) resize(2 * pq.length);

        // add x, and percolate it up to maintain heap invariant
        pq[++n] = x;
        swim(n);
    }

    public int delMax() {
        if (isEmpty())
            return MAGIC_NUMBER;
        int max = pq[1];
        exch(1, n--);
        sink(1);
        pq[n + 1] = MAGIC_NUMBER;     // to avoid loitering and help with garbage collection
        if ((n > 0) && (n == (pq.length - 1) / 4))
            resize(pq.length / 2);
        return max;
    }

    private void swim(int k) {
        while (k > 1 && less(k / 2, k)) {
            exch(k, k / 2);
            k = k / 2;
        }
    }

    private void sink(int k) {
        while (2 * k <= n) {
            int j = 2 * k;
            if (j < n && less(j, j + 1)) j++;
            if (!less(k, j)) break;
            exch(k, j);
            k = j;
        }
    }

    private boolean less(int i, int j) {
        return i < j;
    }

    private void exch(int i, int j) {
        int swap = pq[i];
        pq[i] = pq[j];
        pq[j] = swap;
    }

    public static void main(String[] args) {
        MaxPQ pq = new MaxPQ();
        pq.insert(MAGIC_NUMBER);
        int r = pq.delMax();
        int s = pq.size();
    }

}