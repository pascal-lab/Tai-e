public class Bean {
    A a;

    public Bean(){
        this.a = new A();
    }

    public Bean(A a) {
        this.a = a;
    }

    @org.springframework.context.annotation.Bean
    public Bean2 bean2() {
        // thisVar only points to the Bean3 object
        return new Bean2(this.a);
    }
}
