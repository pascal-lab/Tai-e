@org.springframework.web.bind.annotation.RequestMapping("/path-prefix2")
public interface IController2 {

    @org.springframework.web.bind.annotation.GetMapping(
            "/path/to/get-mapping-endpoint2"
    )
    String getMappingEndpoint2();

}
