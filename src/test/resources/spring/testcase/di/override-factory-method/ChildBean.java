@org.springframework.stereotype.Service
public class ChildBean extends ParentBean {

    @org.springframework.context.annotation.Bean
    public Bean1 bean1() { // this facotry method will not be invoked by child bean (overrided)
        return new Bean1();
    }
}
