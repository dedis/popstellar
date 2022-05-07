package common.net;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;
import io.opencensus.trace.Link;

import java.util.*;

/** A WebSocketClient that can handle multiple received messages */
public class MultiMsgWebSocketClient extends WebSocketClient {

  private final MessageQueue queue;
  private final Logger logger;
  private JsonConverter jsonConverter = new JsonConverter();
  private static final String nonAttendeePk = "oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=";
  private static final String nonAttendeeSkHex = "0cf511d2fe4c20bebb6bd51c1a7ce973d22de33d712ddf5f69a92d99e879363b";
  private ArrayList<Integer> idAssociatedWithSentMessages = new ArrayList<>();

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

  public String getBackendResponseWithBroadcastAndElectionResults(){
    HashSet<String> allAnswers = new HashSet<>();
    String result = "";
    ArrayList<String> broadcasts = new ArrayList<>();
    for (int i = 0; i < 3; i++){
      String answer = getBuffer().takeTimeout(5000);
      if(answer.contains("result")){
        result = answer;
      }
      if (answer.contains("broadcast")){
        broadcasts.add(answer);
      }
    }
    assert broadcasts.size() == 2;
    Base64.Decoder decoder = Base64.getDecoder();
    String base64Data1 = (((LinkedHashMap)((LinkedHashMap)Json.of(broadcasts.get(0)).get("params")).get("message")).get("data").toString());
    String base64Data2 = (((LinkedHashMap)((LinkedHashMap)Json.of(broadcasts.get(1)).get("params")).get("message")).get("data").toString());
    String broadcastData1 = new String(decoder.decode(base64Data1.getBytes()));
    String broadcastData2 = new String(decoder.decode(base64Data2.getBytes()));
    checkResultContainsValidId(result);
    return broadcastData1.contains("result") ? broadcastData1 : broadcastData2;
  }

  private void checkResultContainsValidId(String result){
    Json resultJson = Json.of(result);
    int idResult = resultJson.get("id");
    assert idAssociatedWithSentMessages.contains(idResult);
    idAssociatedWithSentMessages.remove((Integer)idResult);
  }

  public boolean receiveNoMoreResponses(){
    String result = getBuffer().takeTimeout(5000);
    return result == null;
  }
}
