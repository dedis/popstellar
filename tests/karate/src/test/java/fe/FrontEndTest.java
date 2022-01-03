package fe;

import com.intuit.karate.junit5.Karate;

public class FrontEndTest {

  @Karate.Test
  Karate testCreateLAO() {
    return Karate.run("classpath:fe/create_lao.feature");
  }
}
