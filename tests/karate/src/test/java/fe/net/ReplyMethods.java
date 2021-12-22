package fe.net;

import com.intuit.karate.Json;
import common.net.MessageBuffer;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This class contains useful message replies that can be used when using {@link
 * MockBackend#setReplyProducer(Function)}
 */
public class ReplyMethods {

  private static final String VALID_REPLY_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"id\":%ID%,\"result\":0}";
  private static final String VALID_CATCHUP_REPLY_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"id\":%ID%,\"result\":[]}";

  /** Always reply with a valid response */
  public static Function<String, String> ALWAYS_VALID =
      msg -> {
        Json msgJson = Json.of(msg);
        int id = msgJson.get("id");
        String template =
            msgJson.get("method").equals("catchup")
                ? VALID_CATCHUP_REPLY_TEMPLATE
                : VALID_REPLY_TEMPLATE;
        return template.replace("%ID%", Integer.toString(id));
      };
}
