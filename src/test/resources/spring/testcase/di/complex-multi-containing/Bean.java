@org.springframework.stereotype.Component
public class Bean {

    // 在运行时，会根据childBean1和childBean2的注册顺序，只产生一个Bean1对象
    // 因此这个field只会注入一个Bean1对象
    // 但是在目前的分析下，我们没有建模注册顺序，所以会产生两个并注入两个Bean1对象
    @org.springframework.beans.factory.annotation.Autowired
    Bean1 bean1;
}
