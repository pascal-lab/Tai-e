@org.springframework.stereotype.Component
public class Bean3 {
    // 多个构造函数，没有被autowired注解的，同时包含一个空参构造函数
    private Bean bean;

    public Bean3(){

    }

    public Bean3(Bean bean) {
        // not reachable
        this.bean = bean;
    }

}
