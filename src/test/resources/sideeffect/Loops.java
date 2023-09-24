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

public class Loops {
    public static Tree returnLMostLoop(Tree x) {
        while (x.l != null) x = x.l;
        return x;
    }

    public static Tree returnRMostLoop(Tree x) {
        while (x.r != null) x = x.r;
        return x;
    }

    public static Tree returnLMostLoopTmp(Tree x) {
        Tree t = x;
        while (t.l != null) t = t.l;
        return t;
    }

    public static Tree returnLMostLoopTwice(Tree x) {
        while (x.l != null) x = x.l;
        while (x.l != null) x = x.l;
        return x;
    }

    public static Tree returnLMostThenRMostLoop(Tree x) {
        while (x.l != null) x = x.l;
        while (x.r != null) x = x.r;
        return x;
    }

    public static void setXLToNewLoop(Tree x) {
        while (x.r != null) {
            x.l = new Tree(null, null);
            x = x.l;
        }
    }

    ////// Transfer ////////////////////////////////////////////////////////////
    public static Tree returnLMostLoopTransfer(Tree x)
    { return returnLMostLoop(x); }

    public static Tree returnLMostLoopTmpTransfer(Tree x)
    { return returnLMostLoopTmp(x); }

    public static Tree returnLMostLoopThenRMostLoopTransfer(Tree x)
    { return returnLMostThenRMostLoop(x); }

    public static void setXLToNewLoopTransfer(Tree x) { setXLToNewLoop(x); }

    ///// Entry function ///////////////////////////////////////////////////////
    public static void main(String[] args) {
        Tree x = null;

        returnLMostLoop(x);
        returnLMostLoopTmp(x);
        returnLMostLoopTwice(x);
        returnLMostThenRMostLoop(x);
        setXLToNewLoop(x);

        returnLMostLoopTransfer(x);
        returnLMostLoopTmpTransfer(x);
        returnLMostLoopThenRMostLoopTransfer(x);
        setXLToNewLoopTransfer(x);
    }

}