@org.springframework.web.bind.annotation.RestController
public class Controller {

    @org.springframework.web.bind.annotation.RequestMapping("/main")
    public void main(Param1 param1) {
        param1.getParam2().getStr2();
    }

}
