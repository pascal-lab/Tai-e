/*
 * This testcase is taken from https://github.com/saffriha/ictac2014
 */

class Tree {
    public Tree l;
    public Tree r;

    public Tree() { }

    public Tree(Tree l, Tree r) {
        this.l = l;
        this.r = r;
    }
}

public class Globals {
    static Tree global1, global2;

    public static Tree returnGlobal()        { return global1; }
    public static void setXLToGlobal(Tree x) { x.l = global1; }
    public static void setGlobalToX(Tree x)  { global1 = x; }
    public static void setGlobalLToX(Tree x) { global1.l = x; }
    public static void setGlobalOrGlobalLToX(Tree x) {
        if (x == null) global1.l = x;
        else           global1 = x;
    }

    ////// Transfer ////////////////////////////////////////////////////////////

    public static Tree returnGlobalTransfer()        { return returnGlobal(); }
    public static void setXLToGlobalTransfer(Tree x) { setXLToGlobal(x); }
    public static void setGlobalToXTransfer(Tree x)  { setGlobalToX(x); }
    public static void setGlobalLToXTransfer(Tree x) { setGlobalLToX(x); }

    ///// Entry function ///////////////////////////////////////////////////////
    public static void main(String[] args) {
        Tree l = new Tree();
        Tree r = new Tree();
        Tree x = new Tree(l, r);

        setGlobalToX(x);
        setGlobalLToX(x);
        setXLToGlobal(x);
        returnGlobal();

        setGlobalToXTransfer(x);
        setGlobalLToXTransfer(x);
        setXLToGlobalTransfer(x);
        returnGlobalTransfer();
    }
}