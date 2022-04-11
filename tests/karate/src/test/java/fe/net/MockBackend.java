package fe.net;

import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketServerBase;
import common.net.MessageBuffer;
import common.net.MessageQueue;
import common.utils.JsonUtils;
import karate.io.netty.channel.Channel;
import karate.io.netty.channel.ChannelHandlerContext;
import karate.io.netty.channel.SimpleChannelInboundHandler;
import karate.io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

import static common.utils.JsonUtils.*;

/** Defines a mock backend server that is fully customisable. */
public class MockBackend extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private final Logger logger = new Logger(getClass().getSimpleName());

  private final MessageQueue queue;
  private final WebSocketServerBase server;
  // Will be set to true once the connection is established
  private final CompletableFuture<Boolean> connected = new CompletableFuture<>();

  // Defines the rule to apply on incoming messages to produce its reply.
  // Can be null if no reply should be sent back.
  private Function<String, String> replyProducer = ReplyMethods.ALWAYS_VALID;
  private Channel channel;
  Json laoCreationMessageData;

  private static final String VALID_CATCHUP_REPLY_TEMPLATE =
      "{\"jsonrpc\":\"2.0\",\"id\":%ID%,\"result\":[]}";

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
  public void setReplyProducer(Function<String, String> replyProducer) {
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
    queue.onNewMsg(frameText);
    Json json = Json.of(frame.text());


    if (json.get("method").equals("publish")){
      Json paramJson = getJSON(json, "params");
      logger.info("params is {}", paramJson);
//      if (param == null){
//        logger.info("params is null");
//      }
      Json msg = getJSON(paramJson, "message");
      logger.info("msg is {}", msg);
      laoCreationMessageData = msg;
    }

    if (json.get("method").equals("catchup") && laoCreationMessageData != null){
      if (json.toString().contains("consensus")){
        if (replyProducer != null) send(replyProducer.apply(frameText));
      } else {
        String replaceId =
            VALID_CATCHUP_REPLY_TEMPLATE.replace("%ID%", Integer.toString((int) json.get("id")));
        String toSend = replaceId.replace("[]", "[" + laoCreationMessageData.toString() + "]");
        send(toSend);
      }
    } else {
      // Send back the reply
      if (replyProducer != null) send(replyProducer.apply(frameText));
    }
  }

    //    private void broadcastResponse(String frameText) {
  //        JsonObject jsonObject = (JsonObject) JsonParser.parseString(frameText);
  //        if (jsonObject == null){
  //            throw new IllegalStateException();
  //        }
  //        JsonObject msgData = jsonObject.getAsJsonObject("params").getAsJsonObject("")
  //        logger.info("Json extracted is {}", jsonObject.toString());
  //        logger.info("Part is {}", jsonObject.getAsJsonObject("params"));
  //        Gson gson = new Gson();
  //
  //
  //    }

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


}
