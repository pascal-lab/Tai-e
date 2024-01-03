class TestPTAAssert {

    public static void main(String[] args) {
        Object o1 = new Object();
        Object o2 = o1;
        PTAAssert.equals(o1, o2);

        Object o3 = new Object();
        PTAAssert.notEquals(o1, o3);
    }
}
