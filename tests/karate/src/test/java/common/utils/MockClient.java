package common.utils;

import common.model.*;
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

  /**
   * Creates a roll call.
   * @param lao the lao to create the roll call for
   * @return the roll call created
   */
  public RollCall createRollCall(Lao lao) {
    RollCall rollCall = generateValidRollCall(lao);
    return createRollCall(lao, rollCall);
  }

  /**
   * Creates a roll call.
   * @param lao the lao to create the roll call for
   * @param rollCall the roll call to create
   * @return the roll call passed as argument
   */
  public RollCall createRollCall(Lao lao, RollCall rollCall) {
    Map<String, Object> request = new HashMap<>();
    request.put("object", "roll_call");
    request.put("action", "create");
    request.put("id", rollCall.id);
    request.put("name", rollCall.name);
    request.put("creation", rollCall.creation);
    request.put("proposed_start", rollCall.start);
    request.put("proposed_end", rollCall.end);
    request.put("location", rollCall.location);
    request.put("description", rollCall.description);

    this.publish(request, lao.channel);
    this.getBackendResponse(request);

    Map<String, Object> sub = new HashMap<>();
    sub.put("method", "subscribe");
    sub.put("id", 2);
    Map<String, Object> params = new HashMap<>();
    params.put("channel", lao.channel);
    sub.put("params", params);
    sub.put("jsonrpc", "2.0");

    this.send(sub);
    this.takeTimeout(1000);

    return rollCall;
  }

  /**
   * Opens a roll call.
   * @param lao the lao the roll call belongs to
   * @param rollCall the roll call to open
   */
  public void openRollCall(Lao lao, RollCall rollCall) {
    RollCall.RollCallOpen openRollCall = rollCall.open();

    Map<String, Object> request = new HashMap<>();
    request.put("object", "roll_call");
    request.put("action", "open");
    request.put("update_id", openRollCall.updateId);
    request.put("opens", openRollCall.opens);
    request.put("opened_at", openRollCall.openedAt);

    this.publish(request, lao.channel);
    this.getBackendResponse(request);
  }

  /**
   * Closes a roll call.
   * @param lao the lao the roll call belongs to
   * @param rollCall the roll call to close
   */
  public void closeRollCall(Lao lao, RollCall rollCall, List<String> attendees) {
    RollCall.RollCallClose closeRollCall = rollCall.close();

    Map<String, Object> request = new HashMap<>();
    request.put("object", "roll_call");
    request.put("action", "close");
    request.put("update_id", closeRollCall.updateId);
    request.put("closes", closeRollCall.closes);
    request.put("closed_at", closeRollCall.closedAt);
    request.put("attendees", attendees);


    this.publish(request, lao.channel);
    this.getBackendResponse(request);
  }
}
