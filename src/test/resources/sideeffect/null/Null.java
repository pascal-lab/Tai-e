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


public class Null {
    //	@MayAliases(                              {"x.l -> null"})
    public static void setXLToNull(Tree x) { x.l =  null; }

    //	@MayAliases({}) @Returns("null")
    public static Null returnNull() { return null; }


    ////// Transfer ////////////////////////////////////////////////////////////
//	@MayAliases({"x.l -> null"})
    public static void setXLToNullTransfer(Tree x) { setXLToNull(x); }

    //	@MayAliases({}) @Returns("null")
    public static Null returnNullTransfer() { return returnNull(); }


    ///// Entry function ///////////////////////////////////////////////////////
    public static void main(String[] args) {
        Tree x = new Tree(null, null);

        setXLToNull(x);
        returnNull();

        setXLToNullTransfer(x);
        returnNullTransfer();
    }
}