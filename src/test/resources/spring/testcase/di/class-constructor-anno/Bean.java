@org.springframework.stereotype.Service
public class Bean {

    private final Bean2 bean2;

    @org.springframework.beans.factory.annotation.Autowired
    public Bean(Bean2 bean2) {
        this.bean2 = bean2;
    }

    public Bean() { // not reachable
        this.bean2 = null;
    }

}
