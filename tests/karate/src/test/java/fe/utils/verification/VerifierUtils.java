package fe.utils.verification;

import be.utils.JsonConverter;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.subtle.Ed25519Verify;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import static common.JsonKeys.*;
import static common.utils.Base64Utils.convertB64URLToByteArray;

/**
 * This class contains functions useful to test message fields for several kind of high level messages
 */
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
   * @param messageFieldJson the "message" field of the network message
   * @return true if the computed message_id matches the one provided in Json
   */
  private static boolean verifyMessageIdField(Json messageFieldJson) {
    String data = messageFieldJson.get(DATA);
    String signature = messageFieldJson.get(SIGNATURE);
    String msgId = messageFieldJson.get(MESSAGE_ID);
    try {
      return msgId.equals(JsonConverter.hash(data.getBytes(), signature.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  /**
   * Verify the signature of a network message
   * @param messageFieldJson the "message" field of the network message
   * @return true if signature field of the message matches the sender and data
   */
  private static boolean verifySignature(Json messageFieldJson) {
    String senderB64 = messageFieldJson.get(SENDER);
    String signatureB64 = messageFieldJson.get(SIGNATURE);
    String dataB64 = messageFieldJson.get(DATA);

    byte[] sender = convertB64URLToByteArray(senderB64);
    byte[] signature = convertB64URLToByteArray(signatureB64);
    byte[] data = convertB64URLToByteArray(dataB64);

    PublicKeyVerify verify = new Ed25519Verify(sender);

    try {
      verify.verify(signature, data);
      return true;
    } catch (GeneralSecurityException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }
}
