class Node {
    Object item;
    Node next;
}

public class LinkedQueue {
    private int n;         // number of elements on queue
    private Node first;    // beginning of queue
    private Node last;     // end of queue

    public LinkedQueue() {
        first = null;
        last = null;
        n = 0;
    }

    public boolean isEmpty() {
        return first == null;
    }

    public int size() {
        return n;
    }

    public Object peek() {
        if (isEmpty())
            return null;
        return first.item;
    }

    public void enqueue(Object item) {
        Node oldlast = last;
        last = new Node();
        last.item = item;
        last.next = null;
        if (isEmpty())
            first = last;
        else
            oldlast.next = last;
        n++;
    }

    public Object dequeue() {
        if (isEmpty())
            return null;
        Object item = first.item;
        first = first.next;
        n--;
        if (isEmpty())
            last = null;
        return item;
    }

    private boolean check() {
        if (n < 0) {
            return false;
        } else if (n == 0) {
            if (first != null) return false;
            if (last != null) return false;
        } else if (n == 1) {
            if (first == null || last == null) return false;
            if (first != last) return false;
            if (first.next != null) return false;
        } else {
            if (first == null || last == null) return false;
            if (first == last) return false;
            if (first.next == null) return false;
            if (last.next != null) return false;

            // check internal consistency of instance variable n
            int numberOfNodes = 0;
            for (Node x = first; x != null && numberOfNodes <= n; x = x.next) {
                numberOfNodes++;
            }
            if (numberOfNodes != n) return false;

            // check internal consistency of instance variable last
            Node lastNode = first;
            while (lastNode.next != null) {
                lastNode = lastNode.next;
            }
            if (last != lastNode) return false;
        }

        return true;
    }

    public static void main(String[] args) {
        LinkedQueue queue = new LinkedQueue();
        Object e1 = new Object();
        Object e2 = new Object();
        queue.enqueue(e1);
        queue.enqueue(e2);
        queue.check();
        Object item = queue.dequeue();
        PTAAssert.contains(item, e1, e2);
    }
}
