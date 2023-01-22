class UnreachableSwitchBranch2 {

    void tableSwitch() {
        int x = 1;
        int y = x << 2;
        switch (y) {
            case 1:
                use(1);
                break; // unreachable case
            case 2:
                use(2);
                break; // unreachable case
            case 3:
                use(3);
                break; // unreachable case
            case 4:
                use(4);
                break;
        }
    }

    void fallThrough() {
        int x = 1;
        int y = x << 2;
        switch (y) {
            case 2:
                use(2); // unreachable case
            case 4:
                use(4);
            case 8:
                use(8);
                break;
            default:
                use(666); // unreachable case
                break;
        }
    }

    void defaultCase() {
        int x = 1;
        int y = x << 10;
        switch (y) {
            case 2:
                use(2);
                break; // unreachable case
            case 4:
                use(4);
                break; // unreachable case
            case 8:
                use(8);
                break; // unreachable case
            default:
                use(666);
                break;
        }
    }

    void allReach(int x) {
        switch (x) {
            case 2:
                use(2);
                break;
            case 4:
                use(4);
                break;
            case 8:
                use(8);
                break;
            default:
                use(666);
                break;
        }
    }

    void use(int x) {
    }
}
