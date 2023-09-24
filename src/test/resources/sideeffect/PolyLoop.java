public class PolyLoop {

    void test() {
        Node[] nodes = new Node[2];
        nodes[0] = new Body();
        nodes[1] = new Cell();
        for (Node node : nodes) {
            Node n = node;
            if (n instanceof Body) {
                n = (Body) n;
                n.foo();
            }
        }
    }

    public static void main(String[] args) {
        PolyLoop a = new PolyLoop();
        a.test();
    }
}

abstract class Node {
    public A f;

    abstract void foo();
}

class Body extends Node {
    void foo() {

    }
}

class Cell extends Node {
    void foo() {
        this.f = new A();
    }
}


class A {}
