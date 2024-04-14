package be.utils;

import be.model.*;
import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.Logger;
import common.net.MessageQueue;
import common.net.MultiMsgWebSocketClient;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Websocket client capable of creating lao, roll call, and election objects and issuing coins */
public class MockClient extends MultiMsgWebSocketClient {
  public MockClient(String wsURL) {
    super(new WebSocketOptions(wsURL), new Logger(), new MessageQueue());
    System.out.println("Client is connecting to URL: " + wsURL);
  }

  /**
   * @return a valid lao with the client's public key, the current time, and a random valid lao name.
   */
  public Lao generateValidLao() {
    // Name needs to be random so that the same organizer does not create the same lao twice if it happens in the same second
    String randomName = RandomUtils.generateRandomName();
    System.out.println("Client with public key: " + publicKey + " is creating a lao: " + randomName);
    return new Lao(publicKey, Instant.now().getEpochSecond(), randomName);
  }

  /**
   * @param lao the lao to create a roll call for
   * @return a valid roll call for the given lao
   */
  public RollCall generateValidRollCall(Lao lao) {
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
  public Election generateValidElection(Lao lao) {
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

  /**
   * Issues coins to a receiver.
   * So far this is only works for the initial transaction and does not keep track of previous transactions!
   *
   * @param receiver to send the amount to
   * @param amountToGive amount to send
   * @return a valid transaction
   * @throws GeneralSecurityException
   */
  public Transaction issueCoins(MockClient receiver, long amountToGive) throws GeneralSecurityException {
    System.out.println("Client with public key: " + publicKey + " is issuing " + amountToGive + " coins to client with public key: " + receiver.publicKey);
    Transaction transaction = new Transaction();
    transaction.issueInitialCoins(receiver.publicKey, publicKey, privateKey, amountToGive);
    return transaction;
  }

  /**
   * Creates a newly generated lao.
   * @return the lao created
   */
  public Lao createLao() {
    Lao lao = generateValidLao();
    return createLao(lao);
  }

  /**
   * Creates a lao.
   * @param lao the lao to create
   * @return the lao passed as argument
   */
  public Lao createLao(Lao lao) {
    Map<String, Object> request = new HashMap<>();
    request.put("object", "lao");
    request.put("action", "create");
    request.put("id", lao.id);
    request.put("name", lao.name);
    request.put("creation", lao.creation);
    request.put("organizer", lao.organizerPk);
    request.put("witnesses", lao.witnesses);

    this.publish(request, "/root");
    this.getBackendResponse(request);

    return lao;
  }
}
