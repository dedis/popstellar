package fe;

import com.intuit.karate.junit5.Karate;

public class FrontEndTest {

//  @Karate.Test
//  Karate testCreateLAO() {
//    return Karate.run("classpath:fe/LAO/create_lao.feature");
//  }

  @Karate.Test
  Karate testCreateRollCall() {
    return Karate.run("classpath:fe/rollCall/roll_call_creation.feature");
  }
}
