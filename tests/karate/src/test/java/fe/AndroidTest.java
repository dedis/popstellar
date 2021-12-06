package fe;

import com.intuit.karate.junit5.Karate;

class AndroidTest {

  @Karate.Test
  public Karate test() {
    return Karate.run("classpath:fe/android.feature");
  }
}
