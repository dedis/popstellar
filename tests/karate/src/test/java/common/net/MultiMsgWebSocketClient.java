package common.net;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.graal.JsMap;
import net.minidev.asm.ConvertDate;

import java.util.Random;

/** A WebSocketClient that can handle multiple received messages */
public class MultiMsgWebSocketClient extends WebSocketClient {

  private final MessageQueue queue;
  private final Logger logger;

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
    JsonConverter jsonConverter = new JsonConverter();
    Random random = new Random();
    int id = random.nextInt();
    Json request =  jsonConverter.publishМessageFromData(data, id, channel);
    this.send(request.toString());
  }

  public String getBackendResponseWithBroadcast(){
    String answer1 = getBuffer().takeTimeout(5000);
    String answer2 = getBuffer().takeTimeout(5000);
    String result = answer1.contains("result") ? answer1 : answer2;
    return result;
  }

  public String getBackendResponseWithoutBroadcast(){
    String result = getBuffer().takeTimeout(5000);
    return result;
  }

  public boolean receiveNoMoreResponses(){
    String result = getBuffer().takeTimeout(5000);
    return result == null;
  }
}
