public class ParentBean {

    @org.springframework.context.annotation.Bean
    private Bean1 bean1Alias() { // this facotry method will be invoked by child bean
        return new Bean1();
    }

    @org.springframework.context.annotation.Bean
    public Bean1 bean1() { // this facotry method will not be invoked (overrided)
        return new Bean1();
    }

}
