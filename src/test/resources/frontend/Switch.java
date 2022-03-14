class Switch {
    public void f1(int x) {
        switch (x) {
            case 10: return;
            case 100:
                int t = 20;
            case 300:
                int k = 3000;
            case 12213: {
                int j = 3332;
            }
            default: {
                break;
            }
            case 5243:
            case 1312: {
                int a = 123;
            }
        }
    }

    // public void f2(String x) {
    //     switch (x) {
    //         case "13213": {
    //             int t = 20;
    //         }
    //         case "54324":
    //         default: {
    //             int q = 123;
    //         }
    //         case "ayfkl": {
    //             while (x.equals("123")) {
    //                 x = "12";
    //             }
    //         }
    //     }
    //     return;
    // }

    public int f3(int x) {
        switch (x) {
            case 1:
                return 10;
            case 2:
                return 20;
            case 3:
            case 4:
                return 30;
            default:
                return 40;
        }
        return 100;
    }

    enum A {
        SS, CC
    }

    public int f4(A x) {
        switch (x) {
            case SS:
                return 100;
            case CC:
                return 20;
        }
        return 1000;
    }
}