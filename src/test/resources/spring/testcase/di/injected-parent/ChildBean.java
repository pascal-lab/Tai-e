@org.springframework.stereotype.Service
public class ChildBean extends ParentBean {
    @org.springframework.beans.factory.annotation.Autowired
    protected Bean bean1; // points to a Bean instance
}
