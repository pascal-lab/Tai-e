@org.springframework.stereotype.Component
public class Bean4 {
    // 一个非空参构造函数，且没有被@Autowired标记
    private Bean2 bean2;

    public Bean4(Bean2 bean2) {
        this.bean2 = bean2;
    }

}
