package common.net;

import be.utils.JsonConverter;
import be.model.KeyPair;
import be.utils.RandomUtils;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;

import java.util.*;
import java.util.function.Predicate;

/** A WebSocketClient that can handle multiple received messages */
public class MultiMsgWebSocketClient extends WebSocketClient {

  public String publicKey;
  public String privateKey;
  public JsonConverter jsonConverter;

  private final MessageQueue queue;
  public final Logger logger;

  private HashMap<String, Integer> idAssociatedWithSentMessages = new HashMap<>();
  private HashMap<Integer, String> idAssociatedWithAnswers = new HashMap<>();
  private ArrayList<String> broadcasts = new ArrayList<>();

  public MultiMsgWebSocketClient(WebSocketOptions options, Logger logger, MessageQueue queue) {
    super(options, logger);
    this.logger = logger;
    this.queue = queue;

    KeyPair keyPair = new KeyPair();
    this.publicKey = keyPair.getPublicKey();
    this.privateKey = keyPair.getPrivateKey();
    this.jsonConverter = new JsonConverter(publicKey, privateKey);
    System.out.println("Created a MultiMsgWebSocketClient using publicKey: " + publicKey + " and private key: " + privateKey);

    setTextHandler(m -> true);
  }

  public void send(Map<String, Object> jsonDataMap){
    this.send(Json.of(jsonDataMap).toString());
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


  public void publish(Map<String, Object> jsonDataMap, String channel){
    Json dataJson = Json.of(jsonDataMap);
    String data = dataJson.toString();
    Random random = new Random();
    int id = random.nextInt();
    idAssociatedWithSentMessages.put(data, id);
    Json request =  jsonConverter.publishMessageFromData(data, id, channel);
    System.out.println("The final sent request is : " + request.toString());
    this.send(request.toString());
  }

  public String getBackendResponse(Map<String, Object> jsonDataMap){
    return getBackendResponseWithOrWithoutBroadcasts(jsonDataMap, false);
  }

  public String getBackendResponseWithOrWithoutBroadcasts(Map<String, Object> jsonDataMap, boolean withBroadcasts){
    Json dataJson = Json.of(jsonDataMap);
    String data = dataJson.toString();
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
      if (withBroadcasts && answer.contains("broadcast")){
        broadcasts.add(answer);
      }
      answer = getBuffer().takeTimeout(5000);
    }
    assert false;
    throw new IllegalArgumentException("No answer from the backend");
  }

  public String getBackendResponseWithElectionResults(Map<String, Object> jsonDataMap){
    String answer = getBackendResponseWithOrWithoutBroadcasts(jsonDataMap, true);
    Base64.Decoder decoder = Base64.getDecoder();
    for (String broadcast : broadcasts) {
      String base64Data =
          (((LinkedHashMap)
                  ((LinkedHashMap) Json.of(broadcast).get("params")).get("message"))
              .get("data")
              .toString());
      String broadcastData = new String(decoder.decode(base64Data.getBytes()));
      if (broadcastData.contains("result")){
        return broadcastData;
      }
    }
    assert false;
    throw new IllegalArgumentException("No election results where received");
  }

  /**
   * Retrieves all messages with the specified method type from the messages buffer.
   * @param method The method type to filter the messages by.
   * @return A list containing all received messages that match the specified method type.
   */
  public List<String> getMessagesByMethod(String method) {
    List<String> messages = new ArrayList<>();
    Predicate<String> filter = MessageFilters.withMethod(method);

    String message = getBuffer().takeTimeout(5000);
    while (message != null) {
      if (filter.test(message)) {
        messages.add(message);
      }
      message = getBuffer().takeTimeout(5000);
    }
    return messages;
  }

  public boolean receiveNoMoreResponses(){
    String result = getBuffer().takeTimeout(5000);
    return result == null;
  }

  /**
   * Set the client to take a timeout of the given length
   * @param timeout the length to timeout
   */
  public void takeTimeout(long timeout){
    getBuffer().takeTimeout(timeout);
  }

  /**
   * Set the client to use a wrong signature when sending messages
   */
  public void useWrongSignature() {
    String wrongSignature = RandomUtils.generateSignature();
    logger.info("setting wrong signature: " + wrongSignature);
    jsonConverter.setSignature(wrongSignature);
  }

}
