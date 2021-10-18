class UnreachableSwitchBranch {

    void lookupSwitch() {
        int x = 1;
        int y = x << 3;
        switch (y) {
            case 2:
                use(2);
                break;  // unreachable case
            case 4:
                use(4);
                break; // unreachable case
            case 8:
                use(8);
                break;
            default:
                use(666);
                break; // unreachable case
        }
    }

    void use(int x) {
    }
}
