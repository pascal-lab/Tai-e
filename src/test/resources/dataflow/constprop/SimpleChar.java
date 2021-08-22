class SimpleChar {

    void constant() {
        char a = 'a';
        char x = 'x';
    }

    void undefined() {
        char x, y, z;
        z = 'z';
    }

    void propagation() {
        char a = 'a';
        char b = a;
        char c = b;
    }
}
