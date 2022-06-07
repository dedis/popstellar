package be;

import com.intuit.karate.junit5.Karate;

public class ServerTest {
  // this will run all *.feature files that exist in sub-directories
  // see https://github.com/intuit/karate#naming-conventions
//  @Karate.Test
//  Karate testCreateLAO() {
//    return Karate.run("classpath:be/LAO");
//  }
//
//  @Karate.Test
//  Karate testCreateRollCall() {
//    return Karate.run("classpath:be/rollCall/createRollCall.feature");
//  }
//
//  @Karate.Test
//  Karate testOpenRollCall() {
//    return Karate.run("classpath:be/rollCall/openRollCall.feature");
//  }
//
//  @Karate.Test
//  Karate testCloseRollCall() {
//    return Karate.run("classpath:be/rollCall/closeRollCall.feature");
//  }
//
//  @Karate.Test
//  Karate testElectionSetup(){
//    return Karate.run("classpath:be/election/electionSetup.feature");
//  }
//
//    @Karate.Test
//  Karate testElectionOpen(){
//    return Karate.run("classpath:be/election/electionOpen.feature");
//  }
//  @Karate.Test
//  Karate testCastVote(){
//    return Karate.run("classpath:be/election/castVote.feature");
//  }
//
//  @Karate.Test
//  Karate testElectionEnd(){
//    return Karate.run("classpath:be/election/electionEnd.feature");
//  }

  @Karate.Test
  Karate testTransaction(){
    return Karate.run("classpath:be/digitalCash/transaction.feature");
  }
}


