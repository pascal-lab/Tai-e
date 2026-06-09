@org.springframework.context.annotation.Configuration
public class Configuration {

    A a = new A();

    @org.springframework.context.annotation.Bean
    public Bean bean() {
        // thisVar points to the configuration object
        return new Bean(this.a);
    }

}
