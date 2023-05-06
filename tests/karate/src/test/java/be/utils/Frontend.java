package be.utils;

import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.Logger;
import common.net.MessageQueue;
import common.net.MultiMsgWebSocketClient;
import common.utils.Base64Utils;

import java.time.Instant;


public class Frontend extends MultiMsgWebSocketClient {

    String senderPk;
    String privateKeyHex;
    String signature;

    private JsonConverter jsonConverter = new JsonConverter();;


    public Frontend(String wsURL) {
      super(new WebSocketOptions(wsURL), new Logger(), new MessageQueue());

      // How to pass this up to the converter? Keep hardcoded for now
      this.senderPk = "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=";
      this.privateKeyHex = "d257820c1a249652572974fbda9b27a85e54605551c6773504d0d2858d392874";
      this.signature = "ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==";

/*      this.senderPk = Base64Utils.generateSenderPk();
      this.privateKeyHex = Base64Utils.generatePrivateKeyHex();
      this.signature = Base64Utils.generateSignature();*/

    }

    public Lao createLaoWithName(String name){
      return new Lao(senderPk, Instant.now().getEpochSecond(), name);
    }


}
