interface Iterator {
    Object next();
}

class TwoType {
    public static void main(String[] args) {
        new A().a();
        new B().b();
    }
}

class A {
    void a() {
        List l1 = new List();
        l1.add(new Object());
        List l2 = new List();
        l2.add(new Object());

        Iterator i1 = l1.iterator();
        Object o1 = i1.next(); o1.hashCode();
        Iterator i2 = l2.iterator();
        Object o2 = i2.next(); o2.hashCode();
    }
}

class B {
    void b() {
        List l3 = new List();
        l3.add(new Object());

        Iterator i3 = l3.iterator();
        Object o3 = i3.next(); o3.hashCode();
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
