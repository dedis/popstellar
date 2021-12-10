package common.net;

import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A WebSocketClient that can handle multiple received messages
 */
public class MultiMsgWebSocketClient extends WebSocketClient {

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
    try {
      logger.trace("entered listen wait state");
      return queue.lastMessage(timeout);
    } catch (InterruptedException e) {
      logger.error("listen timed out: {}", e + "");
      return null;
    }
  }
}
