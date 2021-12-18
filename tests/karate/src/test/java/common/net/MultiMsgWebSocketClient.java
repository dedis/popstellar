package common.net;

import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;

/** A WebSocketClient that can handle multiple received messages */
public class MultiMsgWebSocketClient extends WebSocketClient {

  private final MessageQueue queue;
  private final Logger logger;

  public MultiMsgWebSocketClient(WebSocketOptions options, Logger logger, MessageQueue queue) {
    super(options, logger);
    this.logger = logger;
    this.queue = queue;
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
}
