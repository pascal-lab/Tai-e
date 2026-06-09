@org.springframework.stereotype.Controller
public class Bean4 extends Bean{

    @org.springframework.beans.factory.annotation.Autowired
    private Bean2 bean2; // points to bean2

}
