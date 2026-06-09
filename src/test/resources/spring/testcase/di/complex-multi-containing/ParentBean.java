public class ParentBean {
    // 在运行时，会根据childBean1和childBean2的注册顺序，只产生一个Bean1对象，但是在目前的分析下，我们没有建模注册顺序，所以会产生两个Bean1对象 (在2-obj下能观察到)
    @org.springframework.context.annotation.Bean
    public Bean1 bean1() {
        return new Bean1();
    }


}
