class MixedDeadCode {

    void foo() {
        int x = 1; // dead assignment
        int y = 2; // dead assignment
        return;
        int z = 3; // unreachable
    }

}
