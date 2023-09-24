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

class SimpleCases {
    public static Tree id(Tree x) {
        return x;
    }

    public static void setXLToY(Tree x, Tree y) {
        x.l = y;
    }

    public static void setXLToNew(Tree x) {
        x.l = new Tree(null, null);
    }

    public static void setXLToNewNew(Tree x) {
        x.l = new Tree(null, null);
        x.l.l = new Tree(null, null);
    }

    public static Tree returnL(Tree x) {
        return x.l;
    }

    public static Tree returnLL(Tree x) {
        return x.l.l;
    }

    public static Tree returnLR(Tree x) {
        return x.l.r;
    }

    public static Tree returnLOrR(Tree x) {
        return (x.l == null) ? x.l : x.r;
    }

    ////// Transfer ////////////////////////////////////////////////////////////
    public static Tree idTransfer(Tree x) {
        return id(x);
    }

    public static void setXLToYTransfer(Tree x, Tree y) {
        setXLToY(x, y);
    }

    public static void setXLToNewTransfer(Tree x) {
        setXLToNew(x);
    }

    public static void setXLToNewNewTransfer(Tree x) {
        setXLToNewNew(x);
    }

    public static Tree returnLTransfer(Tree x) {
        return returnL(x);
    }

    public static Tree returnLTransfer2(Tree x) {
        return returnL(x.r);
    }

    public static Tree returnLLTransfer(Tree x) {
        return returnLL(x);
    }

    public static Tree returnLRTransfer(Tree x) {
        return returnLR(x);
    }

    public static Tree returnLOrRTransfer(Tree x) {
        return returnLOrR(x);
    }
}

public class InterProc {
    ////// Direct recursion ////////////////////////////////////////////////////
    public static Tree returnLMostRecursion(Tree x) {
        if (x.l == null) return x;
        return returnLMostRecursion(x.l);
    }

    ////// Indirect recursion //////////////////////////////////////////////////
    public static Tree returnLRInTurnsLRecursion(Tree x) {
        if (x.l == null) return x;
        return returnLRInTurnsRRecursion(x.l);
    }

    public static Tree returnLRInTurnsRRecursion(Tree x) {
        if (x.r == null) return x;
        return returnLRInTurnsLRecursion(x.r);
    }

    ////// Other ///////////////////////////////////////////////////////////////
    public static void setXRLToYTransfer(Tree x, Tree y)
    { SimpleCases.setXLToY(x.r, y); }

    public static void setXLToYLTransfer(Tree x, Tree y)
    { SimpleCases.setXLToY(x, y.l); }

    public static void resolveToAllocOrFormalTransfer(Tree x, Tree y) {
        if (x.l == null) x = new Tree(null, null);
        SimpleCases.setXLToY(x, y);
    }

    public static void resolveIndirectTransfer(Tree x1, Tree x2, Tree x3, Tree y) {
        if (x1.l == null) x1.l = x2;
        x1.l.l = x3;
        SimpleCases.setXLToY(x1.l.l, y);
    }

    public static void resolveIndirectTransfer2(Tree x1, Tree x2, Tree x3, Tree y) {
        if (x1.l == null) x1.l = x2;
        x1.l.l = x3;
        SimpleCases.setXLToY(x1.l, y);
    }

    public static Tree walkXLRLRMost(Tree x) {
        Tree xl = Loops.returnLMostLoop(x);
        Tree xlr = Loops.returnRMostLoop(xl);
        Tree xlrl = Loops.returnLMostLoop(xlr);
        Tree xlrlr = Loops.returnRMostLoop(xlrl);
        return xlrlr;
    }

    ///// Entry function ///////////////////////////////////////////////////////
    public static void main(String[] args) {
        Tree x = new Tree(null, null);
        Tree y = new Tree(null, null);

        returnLMostRecursion(x);
        returnLRInTurnsLRecursion(x);
        returnLRInTurnsRRecursion(x);
        setXRLToYTransfer(x, y);
        setXLToYLTransfer(x, y);
        resolveToAllocOrFormalTransfer(x, y);
        resolveIndirectTransfer(x, x, x, y);
        resolveIndirectTransfer2(x, x, x, y);
        walkXLRLRMost(x);
    }

}

class Loops {
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
    public static Tree returnLMostLoopTransfer(Tree x) {
        return returnLMostLoop(x);
    }

    public static Tree returnLMostLoopTmpTransfer(Tree x) {
        return returnLMostLoopTmp(x);
    }

    public static Tree returnLMostLoopThenRMostLoopTransfer(Tree x) {
        return returnLMostThenRMostLoop(x);
    }

    public static void setXLToNewLoopTransfer(Tree x) {
        setXLToNewLoop(x);
    }
}