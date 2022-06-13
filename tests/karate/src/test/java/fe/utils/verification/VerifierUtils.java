package fe.utils.verification;

import be.utils.JsonConverter;

import java.security.NoSuchAlgorithmException;

public class VerifierUtils {

  public static boolean verifyMessageId(String base64MsgData, String signature, String msgId)
      throws NoSuchAlgorithmException {
    return msgId.equals(JsonConverter.hash(base64MsgData.getBytes(), signature.getBytes()));
  }
}
