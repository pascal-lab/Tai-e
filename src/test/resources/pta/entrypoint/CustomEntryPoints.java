public class CustomEntryPoints {

    // static entrypoint with empty parameters
    public static void entryWithEmptyParam(Param1 p1, Param1[] p1s) {
        String s1 = p1.getS1(); // Param1.getS1 is not reachable
        p1s[0].setS2(s1); // Param1.setS2 is not reachable
    }

    // static entrypoint with declared parameters
    public static void entryWithDeclaredParam1(Param1 p1, Param1[] p1s) {
        String s1 = p1.getS1(); // Param1.getS1 is reachable
        p1s[0].setS2(s1); // Param1.setS2 is reachable
    }

    // static entrypoint with declared parameters
    public static void entryWithDeclaredParam2(Param2 p2) {
        String s1 = p2.getS1(); // Param2.getS1 is reachable
        Param1 p1 = p2.getP1(); // p1 is not null if k >= 1
        String s2 = p1.getS2(); // Param1.getS2 is reachable if k >= 2
    }

    // instance entrypoint with specified parameters
    public void entryWithSpecifiedParam(Param1 p1, Param1[] p1s, String s) {
        String s1 = p1.getS1(); // Param1.getS1 is reachable
        p1s[0].setS2(s1); // Param1.setS2 is reachable
        s.toString(); // String.toString is reachable
        this.toString(); // Object.toString is reachable
    }

}

class Param1 {

    private String s1;

    private String s2;

    public Param1(String s1, String s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    public String getS1() {
        return s1;
    }

    public String getS2() {
        return s2;
    }

    public void setS1(String s1) {
        this.s1 = s1;
    }

    public void setS2(String s2) {
        this.s2 = s2;
    }

}

class Param2 {

    private final String s1;

    private final Param1 p1;

    public Param2(String s1, Param1 p1) {
        this.s1 = s1;
        this.p1 = p1;
    }

    public String getS1() {
        return s1;
    }

    public Param1 getP1() {
        return p1;
    }

}
