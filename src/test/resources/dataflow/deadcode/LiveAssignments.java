class LiveAssignments {

    void foo(int[] arr, C c) {
        int i = 0;
        arr[i] = 111;
        c.f = 222;
    }

    static class C {
        int f;
    }
}
