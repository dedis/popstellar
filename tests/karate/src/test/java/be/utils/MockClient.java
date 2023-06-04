package be.utils;

import be.model.*;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;
import com.ibm.icu.util.Output;
import com.intuit.karate.driver.Input;
import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.Logger;
import common.net.MessageQueue;
import common.net.MultiMsgWebSocketClient;
import common.utils.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Websocket client capable of creating lao, roll call, and election objects */
public class MockClient extends MultiMsgWebSocketClient {

  public MockClient(String wsURL) {
    super(new WebSocketOptions(wsURL), new Logger(), new MessageQueue());
  }

  /**
   * @return a valid lao with the client's public key, the current time, and a random valid lao name.
   */
  public Lao createValidLao() {
    System.out.println("Client with public key: " + publicKey + " is creating a lao");
    // Name needs to be random so that the same organizer does not create the same lao twice if it happens in the same second
    String randomName = RandomUtils.generateRandomName();
    return new Lao(publicKey, Instant.now().getEpochSecond(), randomName);
  }

  /**
   * @param lao the lao to create a roll call for
   * @return a valid roll call for the given lao
   */
  public RollCall createValidRollCall(Lao lao) {
    System.out.println("Client with public key: " + publicKey + " is creating roll call for lao: " + lao.id);
    long rollCallCreation = Instant.now().getEpochSecond();
    // Name needs to be random so that the same organizer does not create the same roll call twice if it happens in the same second
    String rollCallName = RandomUtils.generateRandomName();
    String rollCallId = RollCall.generateCreateRollCallId(lao.id, rollCallCreation, rollCallName);
    List<String> attendees = Collections.singletonList(publicKey);

    return new RollCall(
      rollCallId,
      rollCallName,
      rollCallCreation,
      rollCallCreation + 100,
      rollCallCreation + 200,
      "valid location",
      "valid description",
      lao.id,
      attendees);
  }

  /**
   * @param lao the lao to create an election for
   * @return a valid empty election for the given lao, questions still need to be added!
   */
  public Election createValidElection(Lao lao) {
    System.out.println("Client with public key: " + publicKey + " is creating an election for lao: " + lao.id);
    long electionCreation = Instant.now().getEpochSecond();
    // Name needs to be random so that the same organizer does not create the same election twice if it happens in the same second
    String electionName = RandomUtils.generateRandomName();
    String electionId = Election.generateElectionSetupId(lao.id, electionCreation, electionName);
    String channel = lao.channel + "/" + electionId;
    return new Election(
      channel,
      electionId,
      electionName,
      "OPEN_BALLOT",
      electionCreation,
      electionCreation + 100,
      electionCreation + 200,
      new ArrayList<>());
  }

  public Transaction issueCoins(MockClient receiver, long amountToGive) throws GeneralSecurityException {
    Transaction transaction = new Transaction(new ArrayList<>(), new ArrayList<>(), 0);
    return transaction.createInitialCoinbaseTransaction(receiver.publicKey, publicKey, privateKey, amountToGive);
  }
}
