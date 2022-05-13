class Zipper {

    public static void main(String[] args) {
        directFlow();
        wrappedFlow();
        unwrappedFlow();
    }

    static void directFlow() {
        Box b = new Box();
        b.set(new Object());
        Object o = b.get();
    }

    static void wrappedFlow() {
        Collection c1 = new Collection();
        String s1 = new String();
        c1.add(s1);
        Iterator i1 = c1.iterator();
        Object o1 = i1.next();

        Collection c2 = new Collection();
        String s2 = new String();
        c2.add(s2);
        Iterator i2 = c2.iterator();
        Object o2 = i2.next();
    }

    static void unwrappedFlow() {
        Box b1 = new Box();
        b1.set(new String());
        SyncBox sb1 = new SyncBox(b1);
        Object o1 = sb1.get();

        Box b2 = new Box();
        b2.set(new String());
        SyncBox sb2 = new SyncBox(b2);
        Object o2 = sb2.get();
    }
}

class Box {

    private Object item;

    void set(Object item) {
        this.item = item;
    }

    Object get() {
        return item;
    }
}

class Collection {

    Object elem;

    void add(Object el) {
        this.elem = el;
    }

    Iterator iterator(){
        Object e = this.elem;
        Iterator itr = new Iterator(e);
        return itr;
    }
}

class Iterator {
    Object next;

    Iterator(Object obj) {
        this.next = obj;
    }

    Object next() {
        return this.next;
    }
}

class SyncBox {

    Box box;

    SyncBox(Box box) {
        this.box = box;
    }

    Object get() {
        synchronized(this) {
            Box b = this.box;
            return b.get();
        }
    }
}
