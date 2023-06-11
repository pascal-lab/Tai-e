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

public class SimpleCases {
    public static Tree id(Tree x) { return x; }
    public static void setXLToY(Tree x, Tree y) { x.l =  y; }
    public static void setXLToNew(Tree x) { x.l = new Tree(null, null); }
    public static void setXLToNewNew(Tree x) {
        x.l = new Tree(null, null);
        x.l.l = new Tree(null, null);
    }
    public static Tree returnL(Tree x) { return x.l; }
    public static Tree returnLL(Tree x) { return x.l.l; }
    public static Tree returnLR(Tree x) { return x.l.r; }
    public static Tree returnLOrR(Tree x) { return (x.l == null) ? x.l : x.r; }

    ////// Transfer ////////////////////////////////////////////////////////////
    public static Tree idTransfer(Tree x) { return id(x); }
    public static void setXLToYTransfer(Tree x, Tree y) { setXLToY(x, y); }
    public static void setXLToNewTransfer(Tree x) { setXLToNew(x); }
    public static void setXLToNewNewTransfer(Tree x)
    { setXLToNewNew(x); }
    public static Tree returnLTransfer(Tree x) { return returnL(x); }
    public static Tree returnLTransfer2(Tree x) { return returnL(x.r); }
    public static Tree returnLLTransfer(Tree x) { return returnLL(x); }
    public static Tree returnLRTransfer(Tree x) { return returnLR(x); }
    public static Tree returnLOrRTransfer(Tree x) { return returnLOrR(x); }

    ///// Entry function ///////////////////////////////////////////////////////
    public static void main(String[] args) {
        Tree x = null;
        Tree y = null;

        id(x);
        setXLToY(x, y);
        setXLToNew(x);
        setXLToNewNew(x);
        returnL(x);
        returnLL(x);
        returnLR(x);
        returnLOrR(x);

        idTransfer(x);
        setXLToYTransfer(x, y);
        setXLToNewTransfer(x);
        setXLToNewNewTransfer(x);
        returnLTransfer(x);
        returnLTransfer2(x);
        returnLLTransfer(x);
        returnLRTransfer(x);
        returnLOrRTransfer(x);
    }
}