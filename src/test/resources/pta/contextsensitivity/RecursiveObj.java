public class RecursiveObj {

    RecursiveObj next;

    public static void main(String[] args) {
        RecursiveObj o1 = new RecursiveObj(null);
        RecursiveObj o2 = new RecursiveObj(o1);
        RecursiveObj o3 = new RecursiveObj(o2);
        RecursiveObj o4 = new RecursiveObj(o3);
        o4.count();
    }

    RecursiveObj(RecursiveObj next) {
        this.next = next;
    }

    int count() {
        if (next == null) {
            return 1;
        }
        return 1 + next.count();
    }

}