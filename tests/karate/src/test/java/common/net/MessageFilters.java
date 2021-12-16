package common.net;

import java.util.function.Predicate;

public class MessageFilters {

  /**
   * Only accept the messages with the given method
   *
   * @param method to accept
   * @return true if the message is accepted
   */
  public static Predicate<String> withMethod(String method) {
    return msg -> msg.contains("\"method\":\"" + method + "\"");
  }
}
