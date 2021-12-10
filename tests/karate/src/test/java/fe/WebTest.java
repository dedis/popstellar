package fe;

import com.intuit.karate.junit5.Karate;

class WebTest {

  @Karate.Test
  public Karate test() {
    return Karate.run("classpath:fe/web.feature");
  }
}
