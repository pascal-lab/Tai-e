@org.springframework.web.bind.annotation.RestController
public class Controller {

    @org.springframework.web.bind.annotation.RequestMapping({
            "/path/to/request-mapping-endpoint",
            "/alias-path/to/request-mapping-endpoint",
    })
    public String requestMappingEndpoint() {
        return "Request Mapping Endpoint";
    }

    @org.springframework.web.bind.annotation.GetMapping(path={
            "/path/to/get-mapping-endpoint",
            "/alias-path/to/get-mapping-endpoint",
    })
    public String getMappingEndpoint() {
        return "Get Mapping Endpoint";
    }

}
