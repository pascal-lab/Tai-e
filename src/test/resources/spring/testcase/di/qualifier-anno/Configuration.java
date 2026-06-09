@org.springframework.context.annotation.Configuration
public class Configuration {

    @org.springframework.context.annotation.Bean
    public Bean beanName1() {
        return new Bean(); // o1
    }

    @org.springframework.context.annotation.Bean({"beanName2", "beanName2Alias"})
    public Bean beanName2Factory() {
        return new Bean(); // o2
    }

    @org.springframework.context.annotation.Bean({"beanName3Alias"})
    public Bean3 bean3Factory() {
        return new Bean3(); // o3
    }

    @org.springframework.context.annotation.Bean
    public Bean2 bean2Name1(
            Bean beanName1, // points to o1
            @org.springframework.beans.factory.annotation.Qualifier("beanName2") Bean _beanName2, // points to o2
            Bean beanName2Alias, // points to o2
            Bean3 beanName3Alias, // points to o3
            @org.springframework.beans.factory.annotation.Qualifier("beanName3") Bean3 bean3 // points to o4
    ) {
        return new Bean2();
    }

}
