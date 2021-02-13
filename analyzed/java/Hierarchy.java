public class Hierarchy {
}

interface I {}

interface II {
    String fii = "fii";
}

interface III extends I, II {}

interface IIII extends III {}

class C {
    String fc;

    String f;
}

class D extends C {}

class E extends C implements I, II {
    String fe;

    String f;
}

class F implements III {}

class G extends E {}
