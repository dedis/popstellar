package common.net;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;

import java.util.LinkedList;
import java.util.Random;

/** A WebSocketClient that can handle multiple received messages */
public class MultiMsgWebSocketClient extends WebSocketClient {

  private final MessageQueue queue;
  private final Logger logger;
  private JsonConverter jsonConverter = new JsonConverter();
  private static final String nonAttendeePk = "oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=";
  private static final String nonAttendeeSkHex = "0cf511d2fe4c20bebb6bd51c1a7ce973d22de33d712ddf5f69a92d99e879363b";
  private LinkedList<Integer> idAssociatedWithSentMessages = new LinkedList<>();

  public MultiMsgWebSocketClient(WebSocketOptions options, Logger logger, MessageQueue queue) {
    super(options, logger);
    this.logger = logger;
    this.queue = queue;

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


  public void publish(String data, String channel){
    Random random = new Random();
    int id = random.nextInt();
    idAssociatedWithSentMessages.add(id);
    Json request =  jsonConverter.publishМessageFromData(data, id, channel);
    this.send(request.toString());
  }

  public void changeSenderToBeNonAttendee(){
    jsonConverter.setSenderSk(nonAttendeeSkHex);
    jsonConverter.setSenderPk(nonAttendeePk);
  }

  public String getBackendResponseWithBroadcast(){
    String answer1 = getBuffer().takeTimeout(5000);
    String answer2 = getBuffer().takeTimeout(5000);
    String result = answer1.contains("result") ? answer1 : answer2;
    String broadcast = answer1.contains("broadcast") ? answer1 : answer2;
    assert broadcast.contains("broadcast");
    checkResultContainsValidId(result);
    return result;
  }

  public String getBackendResponseWithoutBroadcast(){
    String result = getBuffer().takeTimeout(5000);
    checkResultContainsValidId(result);
    return result;
  }

  private void checkResultContainsValidId(String result){
    int id  = idAssociatedWithSentMessages.pop();
    Json resultJson = Json.of(result);
//    assert ((int)resultJson.get("id") == id);
  }

  public boolean receiveNoMoreResponses(){
    String result = getBuffer().takeTimeout(5000);
    return result == null;
  }
}
