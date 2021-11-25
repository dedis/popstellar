package pop;

import com.intuit.karate.junit5.Karate;

public class ServerTest {
    // this will run all *.feature files that exist in sub-directories
    // see https://github.com/intuit/karate#naming-conventions   
    @Karate.Test
    Karate testCreateGo() {

        return Karate.run("classpath:pop/createLAO").tags("@here").karateEnv("go");
    } 

    @Karate.Test
    Karate testCreateScala() {

        return Karate.run("classpath:pop/createLAO").tags("@here").karateEnv("scala");
    } 
}
