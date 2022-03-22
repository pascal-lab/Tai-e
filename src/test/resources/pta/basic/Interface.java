interface Number {
    int get();
}

public class Interface {

    public static void main(String[] args) {
        Number n = new One();
        n.get();
    }
}

class Zero implements Number {

    public int get() {
        return 0;
    }
}

class One implements Number {

    public int get() {
        return 1;
    }
}

class Two implements Number {

    public int get() {
        return 2;
    }
}
