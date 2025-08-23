import java.util.Calendar;
import java.text.SimpleDateFormat;

class ExceptionDate {

    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm";

    class Date extends RuntimeException {
        String date() {
            return ExceptionDate.this.date();
        }
    }

    private String date() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dataFormat = new SimpleDateFormat(DATE_FORMAT_NOW);
        return dataFormat.format(calendar.getTime());
    }

    private static int f(int x, int y) {
        if (x == 0) {
            throw (new ExceptionDate()).new Date();
        }
        return x + y;
    }

    public static void main(String[] args) {
        int [] a = new int[2];
        a[1] = 1;
        try {
            a[1] = 1;
            System.out.print(f(0, 1) + "11" + a[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.print("array out of bounds");
        } catch (Date e) { // should be executed
            System.out.print(e.date());
        }
    }
}
