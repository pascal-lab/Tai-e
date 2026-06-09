@org.springframework.web.bind.annotation.RestController
public class Controller {

    @org.springframework.web.bind.annotation.GetMapping(
            "/path/to/get-mapping-endpoint"
    )
    public String getMappingEndpoint(
            @org.springframework.web.bind.annotation.RequestParam(
                    name = "param1"
            ) String param1,
            @org.springframework.web.bind.annotation.RequestParam(
                    value = "param2"
            ) String param2,
            @org.springframework.web.bind.annotation.RequestParam String param3
    ) {
        return "Get Mapping Endpoint";
    }

}
