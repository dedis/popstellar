package common.net;

import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;

import java.util.List;
import java.util.function.Predicate;

/**
 * A WebSocketClient that can handle multiple received messages
 */
public class MultiMsgWebSocketClient extends WebSocketClient implements MessageBuffer {

  private final MessageQueue queue = new MessageQueue();
  private final Logger logger;

  public MultiMsgWebSocketClient(WebSocketOptions options, Logger logger) {
    super(options, logger);
    this.logger = logger;
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

    if (msg == null)
      logger.error("listen timed out");

    return msg;
  }

  // ========= Delegate functions of MessageBuffer =======

  @Override
  public String peek() {
    return queue.peek();
  }

  @Override
  public String peek(final Predicate<String> filter) {
    return queue.peek(filter);
  }

  @Override
  public List<String> peekAll() {
    return queue.peekAll();
  }

  @Override
  public List<String> peekAll(final Predicate<String> filter) {
    return queue.peekAll(filter);
  }

  @Override
  public List<String> peekN(final int firstN) {
    return queue.peekN(firstN);
  }

  @Override
  public String take() {
    return queue.take();
  }

  @Override
  public String take(final Predicate<String> filter) {
    return queue.take(filter);
  }

  @Override
  public List<String> takeAll() {
    return queue.takeAll();
  }

  @Override
  public List<String> takeAll(final Predicate<String> filter) {
    return queue.takeAll(filter);
  }

  @Override
  public List<String> takeN(final int limit) {
    return queue.takeN(limit);
  }

  @Override
  public String takeTimeout(final long timeout) {
    return queue.takeTimeout(timeout);
  }

  @Override
  public String takeTimeout(final Predicate<String> filter, final long timeout) {
    return queue.takeTimeout(filter, timeout);
  }

  @Override
  public void clear() {
    queue.clear();
  }
}
