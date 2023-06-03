package be.utils;

import be.model.Lao;
import be.model.RollCall;
import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.Logger;
import common.net.MessageQueue;
import common.net.MultiMsgWebSocketClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Websocket client capable of creating lao, roll call, and election objects */
public class MockClient extends MultiMsgWebSocketClient {

  public MockClient(String wsURL) {
    super(new WebSocketOptions(wsURL), new Logger(), new MessageQueue());
  }

  /**
   * @return a valid lao with the client's public key, the current time, and a random valid lao name.
   */
  public Lao createValidLao() {
    System.out.println("Client with public key: " + publicKey + "is creating a lao");
    // Name needs to be random so that the same organizer does not create the same lao twice if it happens in the same second
    String randomName = RandomUtils.generateRandomName();
    return new Lao(publicKey, Instant.now().getEpochSecond(), randomName);
  }

  /**
   * @param lao the lao to create a roll call for
   * @return a valid roll call for the given lao
   */
  public RollCall createValidRollCall(Lao lao) {
    System.out.println("Client with public key: " + publicKey +" is creating roll call for lao: " + lao.id);
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
}
