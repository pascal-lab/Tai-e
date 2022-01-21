@Anno
class Annotated {

    @Anno
    private Object o;

    @IntAnno
//    @IntAnno(1)
    private int i;

    @Anno
    Object foo(@Anno Object p) {
        @Anno Object r = p;
        return r;
    }
}
