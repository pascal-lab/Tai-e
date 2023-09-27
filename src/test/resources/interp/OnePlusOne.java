import java.util.Calendar;
import java.text.SimpleDateFormat;

class OnePlusOne {
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    class T extends RuntimeException {
        String f() {
            return OnePlusOne.this.f();
        }
    }

    private String f() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    private static int f(int x, int y) {
        if (x == 0) {
            throw (new OnePlusOne()).new T();
        }
        return x + y;
    }

    public static void main(String[] args) {
        int [] a = new int[2];
        a[1] = 1;
        try {
            a[1] = 1;
            System.out.println(f(0, 1) + "11" + a[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("xxxx");
        } catch (T e) {
            System.out.println(e.f());
        }
    }
}
