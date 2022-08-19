import java.util.function.Consumer;

/**
 * This case is just to reproduce a bug
 * <br>
 * The cause is that
 * {@link pascal.taie.language.classes.ClassHierarchyImpl#dispatch(
 * pascal.taie.language.classes.JClass, pascal.taie.ir.proginfo.MethodRef)}
 * do not check the subclass relation between the receiver class and the class
 * of the method reference.
 */
public class DispatchBugDueToLackOfSubclassCheck {
    public static void main(String[] args) {
        ClassWithInstanceMethod o1 = new ClassWithInstanceMethod();
        ClassWithStaticMethod o2 = new ClassWithStaticMethod();
        consume(o1, ClassWithInstanceMethod::foo);
        consume(o2, null);
    }

    public static <T> void consume(T o, Consumer<T> consumer) {
        if (consumer == null) {
            return;
        }
        consumer.accept(o);
    }

    static class ClassWithInstanceMethod {
        public void foo() {
        }
    }

    static class ClassWithStaticMethod {
        public static void foo() {
        }
    }
}
