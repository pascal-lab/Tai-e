public interface IController1 extends IIController1 {

    @org.springframework.web.bind.annotation.GetMapping(
            "/path/to/get-mapping-endpoint1"
    )
    String getMappingEndpoint1();

}
