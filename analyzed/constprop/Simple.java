class Simple {
    
    void constant() {
        int x = 1;
        int y = 2;
        int z = 3;
        x = 100;
    }

    void undefined() {
        int x, y, z;
        z = 1;
    }

    void propagation() {
        int x = 10;
        int y = x;
        int z = y;
    }

    void multipleAssigns() {
        int x = 1,y=10;
        x = 2;
        x = 3;
        x = 4;
    }

    void longExpressions() {
        int x = 1, y = 2, z = 3;
        int a = x + y * z;
        int b = (x - y) * z;
    }
}
