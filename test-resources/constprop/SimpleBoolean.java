class SimpleBoolean {

    void constant() {
        boolean t = true;
        boolean f = false;
    }

    void undefined() {
        boolean x, y, z;
        z = true;
    }

    void propagation() {
        boolean a = true;
        boolean b = a;
        boolean c = b;
    }
}
