@org.springframework.context.annotation.Configuration
public class Configuration {

    @org.springframework.context.annotation.Bean
    public static Product product() {
        return new Product();
    }
}
