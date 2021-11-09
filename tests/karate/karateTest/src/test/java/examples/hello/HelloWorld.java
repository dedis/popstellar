package examples.hello;

public class HelloWorld {
    public static String  greet(String name) {

        return name == null || name.isBlank() ?
            "Who are you !" :
            String.format("Hello %s !", name ) ;
    }
}
