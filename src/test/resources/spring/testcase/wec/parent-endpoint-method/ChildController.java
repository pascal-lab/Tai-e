@org.springframework.web.bind.annotation.RestController
public class ChildController extends BaseController  {
// {GET [/path-prefix3/path/to/get-mapping-endpoint3]} BaseController#getMappingEndpoint3
// {GET [/path-prefix3/path/to/override-get-mapping-endpoint4]} ChildController#getMappingEndpoint4

    @org.springframework.web.bind.annotation.GetMapping(
            "/path/to/override-get-mapping-endpoint4"
    )
    String getMappingEndpoint4() {
        return "override-get-mapping-endpoint4";
    }

}
