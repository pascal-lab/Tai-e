public class ParentBean {
    @org.springframework.beans.factory.annotation.Autowired
    protected Bean bean1; // in childbean, this field points to a Bean instance

    @org.springframework.beans.factory.annotation.Autowired
    private Bean bean2; // in childbean, this field points to nothing
}
