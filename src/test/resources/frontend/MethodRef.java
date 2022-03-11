import java.util.Arrays;

// class MethodRef {
//     public void f() {
//         String[] stringArray = { "Barbara", "James", "Mary", "John",
//                 "Patricia", "Robert", "Michael", "Linda" };
//         // Arrays.sort(stringArray, String::compareToIgnoreCase);
//     }
//
//     public int g(int k) {
//         Function<Integer, Integer> a = this::h;
//         return a(k);
//     }
//
//     public int h(int k) {
//         return k + t;
//     }
//
// }

interface Fun<T,R> { R apply(T arg); }

class C {
    int size() { return 0; }
    void test() {
        Fun<C, Integer> f1 = C::size;
    }
}