package fe;

import com.intuit.karate.junit5.Karate;

public class FrontEndTest {

  @Karate.Test
  Karate testCreateLAO() {
    return Karate.run("classpath:fe/LAO/create_lao.feature");
  }

  @Karate.Test
  Karate testCreateRC() {
    return Karate.run("classpath:fe/RollCall/rollCallCreation.feature");
  }

  @Karate.Test
  Karate testOpenRC() {
    return Karate.run("classpath:fe/RollCall/rollCallOpen.feature");
  }

  @Karate.Test
  Karate testCloseRC() {
    return Karate.run("classpath:fe/RollCall/rollCallClose.feature");
  }

  @Karate.Test
  Karate testReopenRC() {
    return Karate.run("classpath:fe/RollCall/rollCallReopen.feature");
  }

  @Karate.Test
  Karate testElectionSetup() {
    return Karate.run("classpath:fe/election/electionSetup.feature");
  }

  @Karate.Test
  Karate testElectionOpen() {
    return Karate.run("classpath:fe/election/electionOpen.feature");
  }

  @Karate.Test
  Karate testCastVote() {
    return Karate.run("classpath:fe/election/castVote.feature");
  }

  @Karate.Test
  Karate testElectionEnd() {
    return Karate.run("classpath:fe/election/electionEnd.feature");
  }
}
