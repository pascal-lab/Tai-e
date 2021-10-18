class PotentialDeadCode {

    int foo;

    int array() {
        int arr[] = { 1, 2, 3 };
        if (arr[0] > arr[1]) {
            return arr[0]; // potential dead code
        } else {
            return arr[1];
        }
    }

    int field() {
        PotentialDeadCode bar = new PotentialDeadCode();
        bar.foo = 1;
        if (bar.foo > 0) {
            return 1;
        }
        bar.foo = 2;  // potential dead code
        return -1;    // potential dead code
    }

}
