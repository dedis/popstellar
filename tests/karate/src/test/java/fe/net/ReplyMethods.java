package fe.net;

import com.intuit.karate.Json;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static common.JsonKeys.*;
import static common.utils.JsonUtils.getJSON;

/**
 * This class contains useful message replies that can be used when using {@link
 * MockBackend#setReplyProducer(Function)}
 */
public class ReplyMethods {

  private static final String VALID_REPLY_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"id\":%ID%,\"result\":0}";
  private static final String VALID_CATCHUP_REPLY_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"id\":%ID%,\"result\":[]}";

  private static final String RC_CREATE_BROADCAST_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"method\": \"broadcast\",\"params\":%PARAM%}";

  private static Json laoCreatePublishJson;

  private static List<String> buildSingleton(String string) {
    return Collections.singletonList(string);
  }

  /** Always reply with a valid response */
  public static Function<String, List<String>> ALWAYS_VALID_CONSENSUS =
      msg -> {
        Json msgJson = Json.of(msg);
        int id = msgJson.get(ID);
        String template =
            msgJson.get(METHOD).equals(CATCHUP)
                ? VALID_CATCHUP_REPLY_TEMPLATE
                : VALID_REPLY_TEMPLATE;
        return buildSingleton(template.replace("%ID%", Integer.toString(id)));
      };

  /** Always reply with a valid response */
  public static Function<String, List<String>> ALWAYS_VALID =
      msg -> {
        Json msgJson = Json.of(msg);
        int id = msgJson.get(ID);
        return buildSingleton(VALID_REPLY_TEMPLATE.replace("%ID%", Integer.toString(id)));
      };

  public static Function<String, List<String>> LAO_CREATE_CATCHUP =
      msg -> {
        if (msg.contains(CONSENSUS) || msg.contains(COIN)) {
          return ALWAYS_VALID_CONSENSUS.apply(msg);
        }
        Json msgJson = Json.of(msg);
        String replaceId =
            VALID_CATCHUP_REPLY_TEMPLATE.replace("%ID%", Integer.toString(msgJson.get(ID)));
        if (laoCreatePublishJson == null) {
          throw new IllegalStateException(
              "When creating a catchup the laoCreate should not be null");
        }
        return buildSingleton(replaceId.replace("[]", "[" + laoCreatePublishJson + "]"));
      };

  public static Function<String, List<String>> LAO_CREATE =
      msg -> {
        Json msgJson = Json.of(msg);
        String method = msgJson.get(METHOD);
        if (PUBLISH.equals(method)) {
          laoCreatePublishJson = getJSON(getJSON(Json.of(msg), PARAMS), MESSAGE);
        }
        if (CATCHUP.equals(method)) {
          return LAO_CREATE_CATCHUP.apply(msg);
        } else { // We want to respond valid result for both publish and subscribe
          return ALWAYS_VALID.apply(msg);
        }
      };

  public static Function<String, List<String>> ROLL_CALL_CREATE_BROADCAST =
      msg -> {
        Json param = getJSON(Json.of(msg), PARAMS);
        String channel = param.get(CHANNEL);

        Map<String, String> msgMap = param.get(MESSAGE);
        Json send = Json.object();
        send.set(CHANNEL, channel);
        send.set(MESSAGE, msgMap);

        String broadCast = RC_CREATE_BROADCAST_TEMPLATE.replace("%PARAM%", send.toString());
        String result = ALWAYS_VALID.apply(msg).get(0);

        return Arrays.asList(broadCast, result);
      };
}
