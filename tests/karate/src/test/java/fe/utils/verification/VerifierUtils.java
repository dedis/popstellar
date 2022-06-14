package fe.utils.verification;

import be.utils.JsonConverter;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.subtle.Ed25519Verify;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import common.utils.Base64Utils;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import static common.JsonKeys.*;

public class VerifierUtils {
  private final static Logger logger = new Logger(VerifierUtils.class.getSimpleName());

  /**
   * Verify the "message" field of a network message json
   * @param paramsFieldJson the Json value of the "params" field
   * @return true if the signature and message_id are correct
   */
  public static boolean verifyMessageField (Json paramsFieldJson){
    Json messageFieldJson = Json.of(paramsFieldJson.get("message"));

    boolean signatureValidity = verifySignature(messageFieldJson);
    boolean messageIdValidity = verifyMessageIdField(messageFieldJson);

    return signatureValidity && messageIdValidity;
  }

  /**
   * Verify the message_id of a network message
   * @param base64MsgData the "data" field
   * @param signature
   * @param msgId
   * @return
   * @throws NoSuchAlgorithmException
   */
  private static boolean verifyMessageId(String base64MsgData, String signature, String msgId)
      throws NoSuchAlgorithmException {
    return msgId.equals(JsonConverter.hash(base64MsgData.getBytes(), signature.getBytes()));
  }

  private static boolean verifyMessageIdField(Json messageFieldJson) {
    String data = messageFieldJson.get(DATA);
    String signature = messageFieldJson.get(SIGNATURE);
    String msgId = messageFieldJson.get(MESSAGE_ID);
    try {
      return VerifierUtils.verifyMessageId(data, signature, msgId);
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  private static boolean verifySignature(Json messageFieldJson) {
    String senderB64 = messageFieldJson.get(SENDER);
    String signature = messageFieldJson.get(SIGNATURE);
    String data = messageFieldJson.get(DATA);
    String sender = Base64Utils.convertB64ToString(senderB64);
    logger.info("sender is " + sender);

    PublicKeyVerify verify = new Ed25519Verify(sender.getBytes());

    try {
      verify.verify(signature.getBytes(), data.getBytes());
      return true;
    } catch (GeneralSecurityException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }
}
