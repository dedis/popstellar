package common.net;

import java.util.function.Predicate;

public class MessageFilters {

  public static Predicate<String> withMethod(String method) {
    return msg -> msg.contains("\"method\":\"" + method + "\"");
  }
}
