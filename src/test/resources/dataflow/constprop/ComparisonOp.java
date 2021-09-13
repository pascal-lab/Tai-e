class ComparisonOp {

    int fcmp(float f1, float f2) {
        int x;
        if (f1 < f2) {
            return -1;
        }
        if (f1 > f2) {
            return 1;
        }
        if (f1 == f2) {
            return 0;
        }
        return 666;
    }

    int dcmp(double d1, double d2) {
        int x;
        if (d1 < d2) {
            return -1;
        }
        if (d1 > d2) {
            return 1;
        }
        if (d1 == d2) {
            return 0;
        }
        return 0;
    }

    int acmp(Object o1, Object o2) {
        if (o1 == null) {
            return 0;
        }
        if (o1 == o2) {
            return 1;
        }
        if (o1 != o2) {
            return -1;
        }
        return 666;
    }
}
