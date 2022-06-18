package fe.net;

import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketServerBase;
import common.net.MessageBuffer;
import common.net.MessageQueue;
import fe.utils.verification.PublishMessageVerification;
import fe.utils.verification.RollCallVerification;
import karate.io.netty.channel.Channel;
import karate.io.netty.channel.ChannelHandlerContext;
import karate.io.netty.channel.SimpleChannelInboundHandler;
import karate.io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static common.JsonKeys.COIN;
import static common.JsonKeys.CONSENSUS;

/** Defines a mock backend server that is fully customisable. */
public class MockBackend extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private final Logger logger = new Logger(getClass().getSimpleName());

  private final MessageQueue queue;
  private final WebSocketServerBase server;
  // Will be set to true once the connection is established
  private final CompletableFuture<Boolean> connected = new CompletableFuture<>();

  // Defines the rule to apply on incoming messages to produce its reply.
  // Can be null if no reply should be sent back.
  private Function<String, List<String>> replyProducer = ReplyMethods.ALWAYS_VALID;
  private Channel channel;
  private Json laoCreationMessageData;

  private String laoID;

  public MockBackend(MessageQueue queue, int port) {
    this.queue = queue;
    server = new WebSocketServerBase(port, "/", this);
    logger.info("Mock Backend started");
  }

  /**
   * Sets the reply producer of the backend.
   *
   * <p>It can be set to null if no reply should be sent back
   *
   * @param replyProducer to set
   */
  public void setReplyProducer(Function<String, List<String>> replyProducer) {
    this.replyProducer = replyProducer;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    channel = ctx.channel();
    connected.complete(true);
    logger.trace("Client connected from the server side");
  }

  // As this object is the channel handler of the server, this function is called whenever a new
  // message is received by it.
  // The text message is held is a TextWebSocketFrame which is the primitive that is sent over the
  // network
  @Override
  protected void channelRead0(
      ChannelHandlerContext channelHandlerContext, TextWebSocketFrame frame) {
    String frameText = frame.text();
    logger.info("message received : {}", frameText);
    if (!frameText.toLowerCase().contains(CONSENSUS) && !frameText.toLowerCase().contains(COIN)) {
      // We don't want consensus or coin messages to interfere since we do not test them yet
      queue.onNewMsg(frameText);
    }
    if (replyProducer != null) replyProducer.apply(frameText).forEach(this::send);
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

  public MessageBuffer getBuffer() {
    return queue;
  }

  public void clearBuffer() {
    logger.info("Buffer cleared");
    queue.clear();
  }

  public boolean receiveNoMoreResponses() {
    return queue.takeTimeout(5000) == null;
  }

  public void setLaoCreateMode() {
    replyProducer = ReplyMethods.LAO_CREATE;
  }

  public void setRollCallCreateMode() {
    replyProducer = ReplyMethods.ROLL_CALL_CREATE_BROADCAST;
  }

  public boolean checkPublishMessage(String message) {
    return PublishMessageVerification.verifyPublishMessage(message);
  }

  public boolean checkRollCallCreateMessage(String message) {
    return RollCallVerification.verifyCreate(message);
  }

  public boolean checkRollCallOpenMessage(String message){
    return RollCallVerification.verifyOpen(message);
  }
}
