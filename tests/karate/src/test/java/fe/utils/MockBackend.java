package fe.utils;

import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketServerBase;

import common.net.MessageQueue;
import karate.io.netty.channel.Channel;
import karate.io.netty.channel.ChannelHandlerContext;
import karate.io.netty.channel.SimpleChannelInboundHandler;
import karate.io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Defines a mock backend server that is fully customisable.
 */
public class MockBackend extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private final MessageQueue queue = new MessageQueue();
  private final WebSocketServerBase server;
  private final Logger logger;

  // Defines the rule to apply on incoming messages to produce its reply.
  // Can be null if no reply should be sent back.
  private Function<String, String> replyProducer;
  private Channel channel;

  public MockBackend(int port, Logger logger) {
    this.logger = logger;
    server = new WebSocketServerBase(port, "/", this);
  }

  /**
   * Set the reply producer of the backend.
   * It can be set to null if no reply should be sent back
   *
   * @param replyProducer to set
   */
  public void setReplyProducer(Function<String, String> replyProducer) {
    this.replyProducer = replyProducer;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    logger.trace("client connected from the server side");
    channel = ctx.channel();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame frame) throws Exception {
    logger.trace("message received : {}", frame.text());
    queue.onNewMsg(frame.text());

    // Send back the reply
    if (replyProducer != null)
      send(replyProducer.apply(frame.text()));
  }

  public int getPort() {
    return server.getPort();
  }

  public void stop() {
    logger.trace("stopping server...");
    server.stop();
  }

  public void send(String text) {
    logger.trace("sending message : {}", text);
    channel.eventLoop().submit(() -> channel.writeAndFlush(new TextWebSocketFrame(text)));
  }

  // ========= Delegate functions of MessageQueue =======

  public List<String> allMessages() {
    return queue.allMessages();
  }

  public List<String> messages(int firstN) {
    return queue.messages(firstN);
  }

  public List<String> messages(Predicate<String> filter) {
    return queue.messages(filter);
  }

  public String lastMessage() {
    return queue.lastMessage();
  }

  public String lastMessage(Predicate<String> filter) {
    return queue.lastMessage(filter);
  }
}
