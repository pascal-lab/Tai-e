class Node {
    String item;
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

    public String peek() {
        if (isEmpty())
            return null;
        return first.item;
    }

    public void enqueue(String item) {
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

    public String dequeue() {
        if (isEmpty())
            return null;
        String item = first.item;
        first = first.next;
        n--;
        if (isEmpty())
            last = null;
        return item;
    }

    public static void main(String[] args) {
        LinkedQueue queue = new LinkedQueue();
        queue.enqueue(SourceSink.source());
        queue.enqueue(SourceSink.source());
        SourceSink.sink(queue.dequeue());
    }
}

