@org.springframework.web.bind.annotation.RequestMapping("/path-prefix3")
public class BaseController {

    @org.springframework.web.bind.annotation.GetMapping(
            "/path/to/get-mapping-endpoint3"
    )
    String getMappingEndpoint3() {
        return "get-mapping-endpoint3";
    }

    @org.springframework.web.bind.annotation.GetMapping(
            "/path/to/get-mapping-endpoint4"
    )
    String getMappingEndpoint4() {
        return "get-mapping-endpoint4";
    }

}
