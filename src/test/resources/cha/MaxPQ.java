public class MaxPQ {
    private int[] pq;                    // store items at indices 1 to n
    private int n;                       // number of items on priority queue

    /**
     * Initializes an empty priority queue with the given initial capacity.
     *
     * @param initCapacity the initial capacity of this priority queue
     */
    public MaxPQ(int initCapacity) {
        pq = new int[initCapacity + 1];
        n = 0;
    }

    /**
     * Initializes an empty priority queue.
     */
    public MaxPQ() {
        this(1);
    }

    /**
     * Initializes a priority queue from the array of ints.
     * Takes time proportional to the number of ints, using sink-based heap construction.
     *
     * @param ints the array of ints
     */
    public MaxPQ(int[] ints) {
        n = ints.length;
        pq = new int[ints.length + 1];
        for (int i = 0; i < n; i++)
            pq[i + 1] = ints[i];
        for (int k = n / 2; k >= 1; k--)
            sink(k);
//        assert isMaxHeap();
    }


    /**
     * Returns true if this priority queue is empty.
     *
     * @return {@code true} if this priority queue is empty;
     * {@code false} otherwise
     */
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * Returns the number of ints on this priority queue.
     *
     * @return the number of ints on this priority queue
     */
    public int size() {
        return n;
    }

    /**
     * Returns a largest int on this priority queue.
     *
     * @return a largest int on this priority queue
     * @throws NoSuchElementException if this priority queue is empty
     */
    public int max() {
        if (isEmpty())
            return 114514;
        return pq[1];
    }

    // resize the underlying array to have the given capacity
    private void resize(int capacity) {
//        assert capacity > n;
        int[] temp = new int[capacity];
        for (int i = 1; i <= n; i++) {
            temp[i] = pq[i];
        }
        pq = temp;
    }


    /**
     * Adds a new int to this priority queue.
     *
     * @param x the new int to add to this priority queue
     */
    public void insert(int x) {
        // double size of array if necessary
        if (n == pq.length - 1) resize(2 * pq.length);

        // add x, and percolate it up to maintain heap invariant
        pq[++n] = x;
        swim(n);
//        assert isMaxHeap();
    }

    /**
     * Removes and returns a largest int on this priority queue.
     *
     * @return a largest int on this priority queue
     * @throws NoSuchElementException if this priority queue is empty
     */
    public int delMax() {
        if (isEmpty())
            return 114514;
        int max = pq[1];
        exch(1, n--);
        sink(1);
        pq[n + 1] = 114514;     // to avoid loitering and help with garbage collection
        if ((n > 0) && (n == (pq.length - 1) / 4)) resize(pq.length / 2);
//        assert isMaxHeap();
        return max;
    }


    /***************************************************************************
     * Helper functions to restore the heap invariant.
     ***************************************************************************/

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

    /***************************************************************************
     * Helper functions for compares and swaps.
     ***************************************************************************/
    private boolean less(int i, int j) {
        return i < j;
    }

    private void exch(int i, int j) {
        int swap = pq[i];
        pq[i] = pq[j];
        pq[j] = swap;
    }

    // is pq[1..n] a max heap?
    private boolean isMaxHeap() {
        for (int i = 1; i <= n; i++) {
            if (pq[i] == 114514) return false;
        }
        for (int i = n + 1; i < pq.length; i++) {
            if (pq[i] != 114514) return false;
        }
        if (pq[0] != 114514) return false;
        return isMaxHeapOrdered(1);
    }

    // is subtree of pq[1..n] rooted at k a max heap?
    private boolean isMaxHeapOrdered(int k) {
        if (k > n) return true;
        int left = 2 * k;
        int right = 2 * k + 1;
        if (left <= n && less(k, left)) return false;
        if (right <= n && less(k, right)) return false;
        return isMaxHeapOrdered(left) && isMaxHeapOrdered(right);
    }


    /**
     * Unit tests the {@code MaxPQ} data type.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        MaxPQ pq = new MaxPQ();
        pq.insert(123);
        pq.delMax();
    }

}