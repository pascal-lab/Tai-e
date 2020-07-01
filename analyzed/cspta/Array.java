class Array {

    public static void main(String[] args) {
        A[] arr = new A[10];
        arr[0] = new A();
        arr[1] = new A();
        A a = arr[0];
        arr.hashCode();
    }
}

class A {
}
