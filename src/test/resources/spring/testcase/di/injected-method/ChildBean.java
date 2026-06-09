@org.springframework.stereotype.Controller
public class ChildBean extends Bean {

    @java.lang.Override
    @org.springframework.beans.factory.annotation.Autowired
    protected void overrideReachable() {
        // this method is reachable for child bean
    }

}
