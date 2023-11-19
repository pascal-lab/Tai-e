public class Main {

    public static void main(String[] args) {
        test1();
    }

    public static void test1(){
        String a = getSourceString();

        String [] b;
        b = a.split(":");

        SourceSink.sink(b[0]);
    }

    public static String getSourceString(){
        return "test1:test2";
    }

}
