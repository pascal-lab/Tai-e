class AllReachableIfBranch {
    int branch(int x, int y) {
        int z = x + y;//dead
        if (x > y) {
            z = 100;
        } else {
            z = 200;
        }
        return z;
    }

    int branch2(int x, int y) {
        int z = x + y;//dead
        int a = x - y;
        if (x > y) {
            a = a + 1;
            z = 100;//dead
        }
        z = 2 + a;
        return z;
    }

}