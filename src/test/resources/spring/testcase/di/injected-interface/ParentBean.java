public interface ParentBean {

    @org.springframework.beans.factory.annotation.Autowired
    default void setter(Bean1 bean1) { // this injected method will be invoked by child bean

    }

    @org.springframework.context.annotation.Bean
    default Bean bean() { // this facotry method will be invoked by child bean
        return new Bean();
    }

}
