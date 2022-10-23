public class DroppedException implements Cloneable {
    public static int x(int i) throws Exception {
        if (i > 0) {
            throw new Exception();
        }
        return i;
    }

    public void commonPractice() { // right case
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

    public DroppedException cloneException() { // should ignore CloneNotSupportedException and InterruptedException
        try {
            return (DroppedException) clone();
        } catch (CloneNotSupportedException e) {
        }
        return new DroppedException();
    }

    public static void emptyCatchBlock1() {
        try {
            x(1);
        } catch (Exception e1) {
        }
    }

    public static void emptyCatchBlock2() {
        try {
            x(0);
        } catch (Exception e2) {
        } catch (Throwable e3) {
        }
    }

    public static void emptyCatchBlockWithFinally1() {
        try {
            x(2);
        } catch (Exception e1) {
        } finally {
            int a = 1;
        }
    }

    public static void emptyCatchBlockWithFinally2() {
        try {
            x(0);
        } catch (Exception e1) {
            // empty catch block with finally block
        } finally {
            try {
                x(1);
            } catch (Exception e2) {
            }
        }
    }

    public int exitInTryBlock() {
        try {
            x(0);
            return 1;
        } catch (Exception e) {
        }

        try {
            return x(2);
        } catch (Exception e) {
        }
        return 0;
    }
}
