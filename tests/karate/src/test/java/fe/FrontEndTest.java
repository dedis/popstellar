package fe;

import com.intuit.karate.junit5.Karate;

public class FrontEndTest {

//  @Karate.Test
//  Karate testCreateLAO() {
//    return Karate.run("classpath:fe/LAO/create_lao.feature");
//  }

  @Karate.Test
  Karate testCreateRC() {
    return Karate.run("classpath:fe/RollCall/rollCallCreation.feature");
  }
}
