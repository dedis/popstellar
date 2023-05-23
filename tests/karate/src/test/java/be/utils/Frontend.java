package be.utils;

import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.Logger;
import common.net.MessageQueue;
import common.net.MultiMsgWebSocketClient;

import java.time.Instant;


public class Frontend extends MultiMsgWebSocketClient {

  public Frontend(String wsURL) {
    super(new WebSocketOptions(wsURL), new Logger(), new MessageQueue());
  }

  public Lao createValidLao() {
    // Name needs to be random so that the same organizer does not create the same lao twice if it happens in the same second
    String randomName = RandomUtils.generateRandomName();
    return new Lao(publicKey, Instant.now().getEpochSecond(), randomName);
  }

  public RollCall createValidRollCall(Lao lao) {
    long rollCallCreation = Instant.now().getEpochSecond();
    String rollCallName = RandomUtils.generateRandomName();
    String rollCallId = RollCall.generateCreateRollCallId(lao.id, rollCallCreation, rollCallName);

    return new RollCall(
      rollCallId,
      rollCallName,
      rollCallCreation,
      rollCallCreation + 100,
      rollCallCreation + 200,
      "valid location",
      "valid description",
      lao.id);
  }

  public void useWrongSignature() {
    String wrongSignature = RandomUtils.generateSignature();
    logger.info("setting wrong signature: " + wrongSignature);
    jsonConverter.setSignature(wrongSignature);
  }
}
