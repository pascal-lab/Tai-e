@org.springframework.stereotype.Component
public class Bean3 {
    @org.springframework.beans.factory.annotation.Autowired
    private Bean bean; // points-to bean1 and bean2

    @org.springframework.beans.factory.annotation.Autowired
    private Bean1 bean1; // points to nothing

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("multi")
    private Bean1 namedBean1; // points to nothing

    @org.springframework.beans.factory.annotation.Autowired
    private Bean2 bean2; // points to nothing

}
