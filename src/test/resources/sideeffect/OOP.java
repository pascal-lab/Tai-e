/*
 * This testcase is taken from https://github.com/saffriha/ictac2014
 */

class Tree {
    public Tree l;
    public Tree r;

    public Tree(Tree l, Tree r) {
        this.l = l;
        this.r = r;
    }
}

public class OOP {
    Tree field1;
    OOP field2 = new OOP();

    public void setFieldToX(Tree x) { this.field1 =  x; }
    public void setXFieldToField(OOP x) { x.field2 =  this.field2; }
    public void setXFieldToThis(OOP x) { x.field2 =  this; }
    public OOP returnThis() { return this; }

    ////// Transfer ////////////////////////////////////////////////////////////
    public void setFieldToXTransferSameObject(Tree x) { setFieldToX(x); }
    public void setFieldToXTransferOtherObject(Tree x, OOP o)
    { o.setFieldToX(x); }

    public void setXFieldToFieldTransferSameObject(OOP x)
    { setXFieldToField(x); }
    public void setXFieldToFieldTransferOtherObject(OOP x, OOP y)
    { y.setXFieldToField(x); }

    public void setXFieldToThisTransferSameObject(OOP x)
    { setXFieldToThis(x); }
    public void setXFieldToThisTransferOtherObject(OOP x, OOP y)
    { y.setXFieldToThis(x); }

    public OOP returnThisTransferSameObject()
    { return returnThis(); }
    public OOP returnThisTransferOtherObject(OOP x)
    { return x.returnThis(); }

    ///// Entry function ///////////////////////////////////////////////////////
    public static void main(String[] args) {
        OOP a = new OOP();
        OOP b = new OOP();
        Tree x = new Tree(null, null);

        a.setFieldToX(x);
        a.setXFieldToField(a);
        a.setXFieldToThis(a);
        a.returnThis();

        a.setFieldToXTransferSameObject(x);
        a.setFieldToXTransferOtherObject(x, b);
        a.setXFieldToFieldTransferSameObject(a);
        a.setXFieldToFieldTransferOtherObject(a, b);
        a.setXFieldToThisTransferSameObject(a);
        a.setXFieldToThisTransferOtherObject(a, b);
        a.returnThisTransferSameObject();
        a.returnThisTransferOtherObject(a);
    }
}