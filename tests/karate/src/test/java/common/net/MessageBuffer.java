package common.net;

import java.util.List;
import java.util.function.Predicate;

public interface MessageBuffer {

  /**
   * @return the first message in the buffer (null is empty)
   */
  String peek();

  /**
   * @param filter of the message, must return true for the valid message
   *               Some useful filters are implemented in {@link MessageFilters}
   * @return the first message in the buffer matching the filter (null if empty)
   */
  String peek(Predicate<String> filter);

  /**
   * @return all the messages left in the buffer
   */
  List<String> peekAll();

  /**
   * @param filter of the message, must return true for the valid message
   *               Some useful filters are implemented in {@link MessageFilters}
   * @return all the messages left in the buffer matching the filter
   */
  List<String> peekAll(Predicate<String> filter);

  /**
   * @return the first n message left in the buffer
   */
  List<String> peekN(int firstN);

  /**
   * Removes and returns the first message left in the buffer.
   * If the buffer is empty, returns null
   *
   * @return the first message in the buffer (null if empty)
   */
  String take();

  /**
   * Removes and returns the first message left in the buffer matching the filter.
   * If no message matches, returns null.
   *
   * @param filter of the message, must return true for the valid message
   *               Some useful filters are implemented in {@link MessageFilters}
   * @return the first message in the buffer matching the filter (null if empty)
   */
  String take(Predicate<String> filter);

  /**
   * Removes and returns all the messages left in the buffer
   *
   * @return all the messages left in the buffer
   */
  List<String> takeAll();

  /**
   * Removes and returns all the messages left in the buffer matching the filter
   *
   * @param filter of the message, must return true for the valid message
   *               Some useful filters are implemented in {@link MessageFilters}
   * @return all the messages left in the buffer matching the filter
   */
  List<String> takeAll(Predicate<String> filter);

  /**
   * Removes and returns the first n messages left in the buffer
   *
   * @param limit of the number to the number of messages the take
   * @return the first n messages left in the buffer
   */
  List<String> takeN(int limit);

  /**
   * Removes and returns the first message  left in the buffer.
   * <p>
   * If the buffer is empty, wait at most timeout for a new message to come.
   * If after the timeout the buffer is still empty, returns null
   *
   * @param timeout in millis
   * @return the first message in the buffer (null if no message until the timeout)
   */
  String takeTimeout(long timeout);

  /**
   * Removes and returns the first message left in the buffer matching the filter.
   * <p>
   * If no match is found, wait at most timeout for a new messages to come.
   * If after the timeout there is still no match, returns null
   *
   * @param filter  of the message, must return true for the valid message
   *                Some useful filters are implemented in {@link MessageFilters}
   * @param timeout in millis
   * @return the first message in the buffer (null if no message until the timeout)
   */
  String takeTimeout(Predicate<String> filter, long timeout);

  /**
   * Clear the buffer and remove any message pending
   */
  void clear();
}
