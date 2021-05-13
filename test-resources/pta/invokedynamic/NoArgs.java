
public class NoArgs {

    public static void main(String[] args){
        Runnable noArguments = () -> System.out.println("Hello World");
        noArguments.run();
    }

}
