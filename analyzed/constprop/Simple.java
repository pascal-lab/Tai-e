class Simple {
    
    void constant() {
        int x = 1;
        int y = 2;
        int z = 3;
        boolean t = true;
        boolean f = false;
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
        boolean a = true;
        boolean b = a;
        boolean c = b; 
    }
}
