package common.net;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/** A WebSocketClient that can handle multiple received messages */
public class MultiMsgWebSocketClient extends WebSocketClient {

  private final MessageQueue queue;
  private final Logger logger;
  private JsonConverter jsonConverter = new JsonConverter();
  private static final String nonAttendeePk = "oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=";
  private static final String nonAttendeeSkHex = "0cf511d2fe4c20bebb6bd51c1a7ce973d22de33d712ddf5f69a92d99e879363b";
  private HashMap<String, Integer> idAssociatedWithSentMessages = new HashMap<>();
  private HashMap<Integer, String> idAssociatedWithAnswers = new HashMap<>();

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
    idAssociatedWithSentMessages.put(data, id);
    Json request =  jsonConverter.publishМessageFromData(data, id, channel);
    this.send(request.toString());
  }

  public void changeSenderToBeNonAttendee(){
    jsonConverter.setSenderSk(nonAttendeeSkHex);
    jsonConverter.setSenderPk(nonAttendeePk);
  }

  public String getBackendResponse(String data){
    assert idAssociatedWithSentMessages.containsKey(data);
    int idData = idAssociatedWithSentMessages.get(data);
    if (idAssociatedWithAnswers.containsKey(idData)){
      String answer = idAssociatedWithAnswers.get(idData);
      idAssociatedWithAnswers.remove(idData);
      idAssociatedWithSentMessages.remove(data);
      return answer;
    }
    String answer = getBuffer().takeTimeout(5000);
    while(answer != null){
      if(answer.contains("result") || answer.contains("error")){
        Json resultJson = Json.of(answer);
        int idResult = resultJson.get("id");
        if (idData == idResult){
          idAssociatedWithSentMessages.remove(data);
          return answer;
        }else{
          idAssociatedWithAnswers.put(idResult, answer);
        }
      }
      answer = getBuffer().takeTimeout(5000);
    }
    assert false;
    throw new IllegalArgumentException("No answer from the backend");
  }

  public boolean receiveNoMoreResponses(){
    String result = getBuffer().takeTimeout(5000);
    return result == null;
  }

  public void setWrongSignature(){
    jsonConverter.setSignature(nonAttendeePk);
  }
}
