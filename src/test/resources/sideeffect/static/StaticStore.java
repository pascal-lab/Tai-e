public class StaticStore {

    public void modA(C c) {
        A.c = c;
    }

    public void mmodA() {
        C c = new C();
        modA(c);
    }

    public static void main(String[] args) {
        StaticStore staticStore = new StaticStore();
        staticStore.mmodA();
    }

}

class A {
    static C c;
}

class C {

}