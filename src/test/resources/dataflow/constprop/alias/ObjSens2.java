interface Iterator {
    Int next();
}

class ObjSens2 {
    public static void main(String[] args) {
        List l1 = new List();
        Int n1 = new Int();
        n1.i = 22;
        l1.add(n1);

        List l2 = new List();
        Int n2 = new Int();
        n2.i = 33;
        l2.add(n2);

        Iterator i1 = l1.iterator();
        Int n3 = i1.next();
        int x = n3.i;
        Iterator i2 = l2.iterator();
        Int n4 = i2.next();
        int y = n4.i;
    }
}

class List {

    Int element;

    void add(Int element) {
        this.element = element;
    }

    Iterator iterator() {
        return new ListIterator();
    }

    class ListIterator implements Iterator {
        public Int next() {
            return element;
        }
    }
}

class Int {
    int i;
}
