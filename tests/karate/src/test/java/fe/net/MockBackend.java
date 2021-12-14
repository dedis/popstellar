package fe.net;

import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketServerBase;

import common.net.MessageBuffer;
import common.net.MessageQueue;
import karate.io.netty.channel.Channel;
import karate.io.netty.channel.ChannelHandlerContext;
import karate.io.netty.channel.SimpleChannelInboundHandler;
import karate.io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Defines a mock backend server that is fully customisable.
 */
public class MockBackend extends SimpleChannelInboundHandler<TextWebSocketFrame> implements MessageBuffer {

  private final Logger logger = new Logger(getClass().getSimpleName());

  private final MessageQueue queue = new MessageQueue();
  private final WebSocketServerBase server;
  // Will be set to true once the connection is established
  private final CompletableFuture<Boolean> connected = new CompletableFuture<>();

  // Defines the rule to apply on incoming messages to produce its reply.
  // Can be null if no reply should be sent back.
  private Function<String, String> replyProducer = ReplyMethods.ALWAYS_VALID;
  private Channel channel;

  public MockBackend(int port) {
    server = new WebSocketServerBase(port, "/", this);
    logger.info("Mock Backend started");
  }

  /**
   * Sets the reply producer of the backend.
   * It can be set to null if no reply should be sent back
   *
   * @param replyProducer to set
   */
  public void setReplyProducer(Function<String, String> replyProducer) {
    this.replyProducer = replyProducer;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    channel = ctx.channel();
    connected.complete(true);
    logger.trace("Client connected from the server side");
  }

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame frame) {
    logger.info("message received : {}", frame.text());
    queue.onNewMsg(frame.text());

    // Send back the reply
    if (replyProducer != null)
      send(replyProducer.apply(frame.text()));
  }

  public int getPort() {
    return server.getPort();
  }

  public boolean waitForConnection(long timeout) {
    logger.info("Waiting for connection...");
    long start = System.currentTimeMillis();
    try {
      connected.get(timeout, TimeUnit.MILLISECONDS);
      logger.info("Connection established in {}s", (System.currentTimeMillis() - start) / 1000.0);
      return true;
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      return false;
    } catch (TimeoutException e) {
      logger.error("timeout while waiting for connection to backend");
      return false;
    }
  }

  public boolean isConnected() {
    return connected.isDone();
  }

  public void stop() {
    logger.info("stopping server...");
    server.stop();
  }

  public void send(String text) {
    logger.info("sending message : {}", text);
    channel.eventLoop().submit(() -> channel.writeAndFlush(new TextWebSocketFrame(text)));
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
