@org.springframework.web.bind.annotation.RestController
public class RichController extends BaseController implements IController1, IController2 {
// {GET [/path-prefix1/path/to/get-mapping-endpoint1]} RichController#getMappingEndpoint1
// {GET [/path-prefix1/path/to/get-mapping-endpoint2]} RichController#getMappingEndpoint2
// {GET [/path-prefix1/path/to/get-mapping-endpoint3]} BaseController#getMappingEndpoint3
// {GET [/path-prefix1/path/to/get-mapping-endpoint4]} RichController#getMappingEndpoint4

    @Override
    public String getMappingEndpoint1() {
        return "Get Mapping Endpoint 1";
    }

    @Override
    public String getMappingEndpoint2() {
        return "Get Mapping Endpoint 1";
    }

    @Override
    public String getMappingEndpoint4() {
        return "get-mapping-endpoint4";
    }

}
