class ArrayFieldTransfer {

    public static void main(String args[]) {
        simpleTaint();
        arrayToVar();
        fieldToVar();
        varToArray();
        varToField();
    }

    private static A getSource() {
        return new C();
    }

    private static void simpleTaint() {
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

    private static void arrayToVar() {
        A taint = getSource();
        Expression exp = new Expression(new Object[]{taint});
        exp.getValue();
    }

    private static void fieldToVar() {
        A taint = getSource();
        Expression exp = new Expression(new ABox(taint));
        exp.getValue();
    }

    private static void varToArray() {
        A taint = getSource();
        A[] a = new A[1];
        transfer(taint, a)
        B b = new B();
        b.sink(a[0]);
    }

    private static void transfer(A a, A[] array) {
    }


    private static void varToField() {
        A taint = getSource();
        Expression exp = new Expression();
        transfer(taint, exp);
        B b = new B();
        b.sink(exp.a);
    }

    private static void transfer(A a, Expression exp) {
    }
}

abstract class A {
    abstract void sink(A a);

    public void transfer(A taint) {
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

class ABox {
    A a;

    ABox(A a) {
        this.a = a;
    }
}

class Expression {

    Object[] cmds;

    A a;

    Expression() {
    }

    Expression(Object[] cmds) {
        this.cmds = cmds;
    }

    Expression(ABox abox) {
        this.a = abox.a;
    }

    Object getValue() {
        return "value";
    }
}
