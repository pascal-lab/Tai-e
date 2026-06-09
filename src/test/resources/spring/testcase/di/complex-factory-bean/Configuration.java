@org.springframework.context.annotation.Configuration
public class Configuration {

    @org.springframework.context.annotation.Bean
    public Bean multi(int x) {
        if (x == 1) return new Bean1();
        return new Bean2();
    }

}
