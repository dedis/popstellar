package common.net;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;

/** A WebSocketClient that can handle multiple received messages */
public class MultiMsgWebSocketClient extends WebSocketClient {

  private final MessageQueue queue;
  private final Logger logger;
  private JsonConverter jsonConverter;
  private final String nonAttendeePk = "oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=";
  private final String nonAttendeeSkHex = "0cf511d2fe4c20bebb6bd51c1a7ce973d22de33d712ddf5f69a92d99e879363b";

  public MultiMsgWebSocketClient(WebSocketOptions options, Logger logger, MessageQueue queue) {
    super(options, logger);
    this.logger = logger;
    this.queue = queue;
    this.jsonConverter =  new JsonConverter();

    setTextHandler(m -> true);
  }

  @Override
  public void signal(Object result) {
    logger.trace("signal called: {}", result);
    queue.onNewMsg(result.toString());
  }

  @Override
  public synchronized Object listen(long timeout) {
    logger.trace("entered listen wait state");
    String msg = queue.take();

    if (msg == null) logger.error("listen timed out");

    return msg;
  }

  public MessageBuffer getBuffer() {
    return queue;
  }

  public void publish(String data, int id, String channel){
    Json request =  jsonConverter.publishМessageFromData(data, id, channel);
    this.send(request.toString());
  }

  public void setNonAttendeeAsSender(){
    jsonConverter.setSenderSk(nonAttendeeSkHex);
    jsonConverter.setSenderPk(nonAttendeePk);
  }

  public String getBackendResponseWithBroadcast(){
    String broadcast = getBuffer().takeTimeout(5000);
    String result = getBuffer().takeTimeout(5000);
    return result;
  }

  public String getBackendResponseWithoutBroadcast(){
    String result = getBuffer().takeTimeout(5000);
    return result;
  }

  public boolean receiveNoMoreResponses(){
    String result_dummy = getBuffer().takeTimeout(5000);
    String result = getBuffer().takeTimeout(5000);
    return result == null;
  }
}
