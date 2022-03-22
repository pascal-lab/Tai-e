@Year(2022)
@Author(@Name(family = "Tan", given = "Tian"))
class AnnotatedJava {

    @Anno
    @Year
    private Object o;

    @Year(1999)
    private int i;

    @Cards({ Mahjong.FENG, Mahjong.TIAO })
    private String s;

    Object foo(@Anno Object p1, int p2, @Copyright("Free") String p3) {
        @Anno Object r = p1;
        return r;
    }

    @ClassAnno(String.class)
    @Copyright("2022 PASCAL group")
    void baz() {
    }

    void notAnnotated() {
    }
}

@interface Anno {
}

@interface ClassAnno {
    Class<?> value();
}

@interface Year {
    int value() default 0;
}

@interface Copyright {
    String value();
}

@interface Name {
    String family();

    String given();
}

@interface Author {
    Name value();
}

enum Mahjong {
    TIAO, WAN, TONG, FENG, JIAN
}

@interface Cards {
    Mahjong[] value();
}
