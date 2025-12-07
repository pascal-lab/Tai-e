public class DroppedException implements Cloneable {

    public static int x(int i) throws Exception {
        if (i > 0) {
            throw new Exception();
        }
        return i;
    }

    public void commonPractice() {
        try {
            x(1);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                x(-1);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void commonPracticeWithFinally() {
        try {
            x(1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            int i = 0;
        }
    }

    public DroppedException cloneException() {
        try {
            return (DroppedException) clone();
        } catch (CloneNotSupportedException e) { // DE_MIGHT_DROP
            throw new RuntimeException("Clone failed");
        }
    }

    public DroppedException cloneException2() {
        try {
            return (DroppedException) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

    public static void emptyCatchBlock1() {
        try {
            x(1);
        } catch (Exception e1) { // DE_MIGHT_IGNORE
        }
    }

    public static void emptyCatchBlock2() {
        try {
            x(0);
        } catch (Exception e2) { // DE_MIGHT_IGNORE
        } catch (Throwable e3) { // DE_MIGHT_IGNORE
        }
    }

    public static void emptyCatchBlockWithFinally1() {
        try {
            x(2);
        } catch (Exception e1) { // DE_MIGHT_IGNORE
        } finally {
            int a = 1;
        }
    }

    public static void emptyCatchBlockWithFinally2() {
        try {
            x(0);
        } catch (Exception e1) { // DE_MIGHT_IGNORE
        } finally {
            try {
                x(1);
            } catch (Exception e2) { // DE_MIGHT_IGNORE
            }
        }
    }

    public int exitInTryBlock() {
        try {
            x(0);
            return 1;
        } catch (Exception e) { // DE_MIGHT_IGNORE
        }

        try {
            return x(2);
        } catch (Exception e) { // DE_MIGHT_IGNORE
        }
        return 0;
    }

}
