@org.springframework.stereotype.Controller
public class Bean {

    @org.springframework.beans.factory.annotation.Autowired
    public void reachable() {
        // this method is reachable for Bean and child bean
    }

    @org.springframework.beans.factory.annotation.Autowired
    private void privateReachable() {
        // this method is reachable for Bean and child bean
    }

    @org.springframework.beans.factory.annotation.Autowired
    protected void overrideReachable() {
        // this method is reachable for Bean and child bean
    }

    public void nonReachable() {
    }

}
