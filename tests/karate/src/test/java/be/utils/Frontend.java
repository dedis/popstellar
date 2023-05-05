package be.utils;

import com.intuit.karate.Json;
import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.Logger;
import common.net.MessageBuffer;
import common.net.MessageQueue;
import common.net.MultiMsgWebSocketClient;

import java.util.Map;

public class Frontend {
  private MultiMsgWebSocketClient multiMsgSocket;

  public Frontend(String wsURL) {
    Logger logg = new Logger();
    MessageQueue q = new MessageQueue();
    WebSocketOptions wso = new WebSocketOptions(wsURL);
    this.multiMsgSocket = new MultiMsgWebSocketClient(wso, logg, q);
  }

  public void send(Map<String, Object> jsonDataMap){
    this.multiMsgSocket.send(jsonDataMap);
  }

  public void publish(Map<String, Object> jsonDataMap, String channel) {
    this.multiMsgSocket.publish(jsonDataMap, channel);
  }

  public String getBackendResponse(Map<String, Object> jsonDataMap) {
    return this.multiMsgSocket.getBackendResponse(jsonDataMap);
  }

  public boolean receiveNoMoreResponses() {
    return this.multiMsgSocket.receiveNoMoreResponses();
  }

  public void close() {
    this.multiMsgSocket.close();
  }

  public MessageBuffer getBuffer() {
    return this.multiMsgSocket.getBuffer();
  }

}
