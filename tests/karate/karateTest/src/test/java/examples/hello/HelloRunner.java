package examples.hello;

import com.intuit.karate.junit5.Karate;

class HelloRunner {

    @Karate.Test
    Karate testUsers() {
        
        return Karate.run("hello").relativeTo(getClass());
    }  
    
}
