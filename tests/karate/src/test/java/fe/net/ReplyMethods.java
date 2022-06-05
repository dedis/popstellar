package fe.net;

import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import common.net.MessageBuffer;
import common.utils.JsonUtils;

import java.util.function.Function;
import java.util.function.Predicate;

import static common.utils.JsonUtils.getJSON;

/**
 * This class contains useful message replies that can be used when using {@link
 * MockBackend#setReplyProducer(Function)}
 */
public class ReplyMethods {
  private static final String SUBSCRIBE = "subscribe";

  private static final String CATCHUP = "catchup";

  private static final String BROADCAST = "broadcast";

  private static final String ID = "id";

  private static final String MESSAGE = "message";

  private static final String PARAMS = "params";

  private static final String METHOD = "method";

  private static final String PUBLISH = "publish";

  private static final String CONSENSUS = "consensus";

  private static final String VALID_REPLY_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"id\":%ID%,\"result\":0}";
  private static final String VALID_CATCHUP_REPLY_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"id\":%ID%,\"result\":[]}";

  private static final String RC_CREATE_BROADCAST_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"method\": \"broadcast\",\"params\":%PARAM%}";

  private static Json laoCreatePublishJson;

  /** Always reply with a valid response */
  public static Function<String, String> ALWAYS_VALID_CONSENSUS =
      msg -> {
        Json msgJson = Json.of(msg);
        int id = msgJson.get("id");
        String template =
            msgJson.get("method").equals("catchup")
                ? VALID_CATCHUP_REPLY_TEMPLATE
                : VALID_REPLY_TEMPLATE;
        return template.replace("%ID%", Integer.toString(id));
      };

  /** Always reply with a valid response */
  public static Function<String, String> ALWAYS_VALID =
      msg -> {
        Json msgJson = Json.of(msg);
        int id = msgJson.get(ID);
        return VALID_REPLY_TEMPLATE.replace("%ID%", Integer.toString(id));
      };

  public static Function<String, String> LAO_CREATE_CATCHUP =
      msg ->{
        if (msg.contains(CONSENSUS)){
          return ALWAYS_VALID_CONSENSUS.apply(msg);
        }
        Json msgJson = Json.of(msg);
        String replaceId =
            VALID_CATCHUP_REPLY_TEMPLATE.replace("%ID%", Integer.toString((int) msgJson.get("id")));
        if (laoCreatePublishJson == null){ // Should not happen
          return replaceId;
        }
        return replaceId.replace("[]", "[" + laoCreatePublishJson.toString() + "]");
      };


  public static Function<String, String> LAO_CREATE =
      msg ->{
        Json msgJson = Json.of(msg);
        String method = msgJson.get(METHOD);
        if (PUBLISH.equals(method)){
          laoCreatePublishJson = getJSON(getJSON(Json.of(msg), "params"), "message");
        }
        if (CATCHUP.equals(method)){
          return LAO_CREATE_CATCHUP.apply(msg);
        }
        else { // We want to respond valid result for both publish and subscribe
          return ALWAYS_VALID.apply(msg);
        }
      };

  public static Function<String, String>  ROLL_CALL_CREATE_BROADCAST =
      msg ->{
        Json param = getJSON(Json.of(msg), "params");
        String channel = param.get("channel");
        Json jsonMsg = getJSON(param, "message");
        Json send = Json.object();
        send.set("channel", channel);
        send.set("message", jsonMsg);
        return RC_CREATE_BROADCAST_TEMPLATE.replace("%PARAM%", send.toString());
      };
}
