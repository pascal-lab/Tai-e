class TaintTransferEdge {

    public static void main(String args[]) {
        simpleTaint();
        transferArray();
    }

    public static A getSource() {
        return new C();
    }

    public static void simpleTaint() {
        B b = new B(); //OB
        A taint = getSource();
        taint = b;

        C c = new C();

        A merge;
        merge.transfer(taint);
        merge = c;

        merge.sink(merge); // will not cause taint flow, because OB is not in merge's pts

        A merge2 = taint;
        merge2 = c;
        merge2.sink(merge2); //merge2 will cause taint flow (call sink method B.sink())
    }

    public static void transferArray() {
        A taint = getSource();
        Expression exp = new Expression(new Object[]{taint});
        exp.getValue();
    }
}

abstract class A {
    abstract void sink(A a);

    public void transfer(A taint) {
        ;
    }

}

class B extends A {
    @Override
    void sink(A a) {
    }
}

class C extends A {
    @Override
    void sink(A a) {
    }
}

class Expression {

    Object[] cmds;

    Expression(Object[] cmds) {
        this.cmds = cmds;
    }

    Object getValue() {
        return "value";
    }
}
