package common.net;

import com.intuit.karate.Json;

import java.util.function.Predicate;

/**
 * This class contains useful message filters that can be used when using :
 * <li>{@link MessageBuffer#peek(Predicate)}
 * <li>{@link MessageBuffer#peekAll(Predicate)}
 * <li>{@link MessageBuffer#take(Predicate)}
 * <li>{@link MessageBuffer#takeAll(Predicate)}
 * <li>{@link MessageBuffer#takeTimeout(Predicate, long)}
 */
public final class MessageFilters {

  private MessageFilters() {
    // Static class, this should never be called
  }

  /**
   * Only accept the messages with the given method
   *
   * @param method to accept
   * @return true if the message is accepted
   */
  public static Predicate<String> withMethod(String method) {
    return msg -> Json.of(msg).getOptional("method").map(method::equals).orElse(false);
  }
}
