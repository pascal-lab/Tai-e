public class NullDeref2 {

    void possibleNullOnSplit() {
        Object a = null;
        if (a == null) {// report a RCN warning
            try {
                System.out.println(1);
            } catch (RuntimeException e) {
                a = "notNull";
            }

            System.out.println(a.getClass());// report an NP warning
        }
    }

    static boolean same0(Object a, Object b) {
        if ((a == null && b == null) || b.equals(a))
            return true;
        else
            return false;
    }

    static boolean same1(int a[], int b[]) {
        if (a == null)
            if (b == null || b.length == 0)
                return true;
        if (b == null)
            if (a == null || a.length == 0)
                return true;
        // Bug is here. If one but not both of a & b are null,
        // we will get a null pointer exception
        if (a.length != b.length)
            return false;
        return true;
    }

    static boolean same2(Object a, Object b) {
        if ((null == a && null == b) || a.equals(b)) // report a RCN warning
            return true;
        else
            return false;
    }

    static boolean same3(Object a, Object b) {
        if ((a == null && b == null) || a.equals(b)) // report a RCN warning
            return true;
        else
            return false;
    }

}
