interface Iterator {
    Object next();
}

class TwoObject {
    public static void main(String[] args) {
        m();
    }

    static void m() {
        List l1 = new List();
        l1.add(new Object());
        List l2 = new List();
        l2.add(new Object());

        Iterator i1 = l1.iterator();
        Object o1 = i1.next();
        Iterator i2 = l2.iterator();
        Object o2 = i2.next();
    }
}

class List {

    Object element;

    void add(Object e) {
        this.element = e;
    }

    Iterator iterator() {
        return new ListIterator();
    }

    class ListIterator implements Iterator {

        public Object next() {
            return element;
        }
    }
}
