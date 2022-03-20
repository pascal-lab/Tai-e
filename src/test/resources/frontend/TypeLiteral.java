class TypeLiteral {
    void method(Object b) {
        if(b.getClass() == TypeLiteral.class) {
            TypeLiteral f = TypeLiteral.class.cast(b);
        }
    }
}