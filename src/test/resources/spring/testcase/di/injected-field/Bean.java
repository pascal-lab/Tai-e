@org.springframework.stereotype.Controller
public class Bean {

    @org.springframework.beans.factory.annotation.Autowired
    private Bean2 bean2;

    private Bean3 bean3; // points to nothing

    @org.springframework.beans.factory.annotation.Autowired
    private ParentBean parentBean;  // points to child bean
}
