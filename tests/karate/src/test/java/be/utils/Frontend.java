package be.utils;

import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.Logger;
import common.net.MessageQueue;
import common.net.MultiMsgWebSocketClient;
import common.utils.Base64Utils;

import java.time.Instant;


public class Frontend extends MultiMsgWebSocketClient {

    public Frontend(String wsURL) {
      super(new WebSocketOptions(wsURL), new Logger(), new MessageQueue());

    }

  public Lao createValidLao(){
    return new Lao(senderPk, Instant.now().getEpochSecond(), "valid lao name");
  }

}
