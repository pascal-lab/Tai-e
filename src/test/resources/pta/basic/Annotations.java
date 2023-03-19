import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;

class Annotations {

    public static void main(String[] args) throws Exception {
        Method foo = A.class.getMethod("foo", String.class, Object.class);
        Annotation[][] annotations = foo.getParameterAnnotations();
        System.out.println(Arrays.deepToString(annotations));
        Annotation a = annotations[0][1];
        Class<?> annoType = a.annotationType();
        // System.out.println(annoType);
        ClassAnno classAnno = annoType.getAnnotation(ClassAnno.class);
        // System.out.println(classAnno);
        Class<?> value = classAnno.value();
        // System.out.println(value);
    }
}

class A {

    public void foo(@ClassAnno(B.class) @StringAnno("xxx") String s,
                    @ClassAnno(A.class) Object o) {
    }
}

class B {
}

@Retention(RetentionPolicy.RUNTIME)
@interface ClassAnno {
    Class<?> value();
}

@Retention(RetentionPolicy.RUNTIME)
@ClassAnno(Annotations.class)
@interface StringAnno {
    String value() default "";
}
