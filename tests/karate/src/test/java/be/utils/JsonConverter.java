package be.utils;

import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;
import com.intuit.karate.Json;
import common.utils.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonConverter {

  public String publicKey;
  public String privateKey;

  private String signatureForced;
  private boolean isSignatureForced = false;
  private String messageIdForced = "";

  public JsonConverter(String publicKey, String privateKey){
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  /**
   * Produces a valid Json representation of a publish message given the high level message data, the id of the message
   * and the channel where the message is supposed to be sent
   */
  public Json constructPublishMessage(String highLevelMessageData, int messageId, String channel) {
    Json publishMessage = Json.object();
    publishMessage.set("jsonrpc", "2.0");
    publishMessage.set("id", messageId);
    publishMessage.set("method", "publish");

    Map<String, Object> paramsField = new LinkedHashMap<>();
    try{
    paramsField = constructParamsField(channel, highLevelMessageData);
    publishMessage.set("params", paramsField);
    }catch (GeneralSecurityException e){
      e.printStackTrace();
    }
    return publishMessage;
  }

  /**
   * Creates a JSON map of the params field of a publish message.
   * Consists of sub-fields channel and message.
   */
  public Map<String, Object> constructParamsField(String channel, String highLevelMessageData) throws GeneralSecurityException {
    Map<String, Object> paramsField = new LinkedHashMap<>();
    Map<String, Object> messageField = constructMessageField(highLevelMessageData);

    paramsField.put("channel", channel);
    paramsField.put("message", messageField);

    return paramsField;
  }

  /**
   * Creates a JSON map of the message field of a publish message.
   * Consists of sub-fields data, sender, signature, message id and witness signatures.
   */
  public Map<String, Object> constructMessageField(String highLevelMessageData) throws GeneralSecurityException{
    Map<String, Object> messageField = new LinkedHashMap<>();
    Json messageData = Json.of(highLevelMessageData);
    String messageDataBase64 = Base64Utils.convertJsonToBase64(messageData);
    String signature = constructSignature(highLevelMessageData);
    if(isSignatureForced){
      signature = this.signatureForced;
      isSignatureForced = false;
    }
    String messageId = Hash.hash(messageDataBase64.getBytes(), signature.getBytes());
    String[] witness = new String[0];

    messageField.put("data", messageDataBase64);
    messageField.put("sender", publicKey);
    messageField.put("signature", signature);
    messageField.put("message_id", messageId);
    messageField.put("witness_signatures", witness);

    return messageField;
  }

  /** Constructs a valid signature on given data */
  public String constructSignature(String messageData) throws GeneralSecurityException {
      PublicKeySign publicKeySign = new Ed25519Sign(Base64Utils.decode(privateKey));
      byte[] signBytes = publicKeySign.sign(messageData.getBytes(StandardCharsets.UTF_8));
      return Base64Utils.encode(signBytes);
  }

  /**
   * If we want to test having a signature that does not match the data and private key we can set
   * it by force
   */
  public void setSignature(String newSignature) {
    isSignatureForced = true;
    this.signatureForced = newSignature;
  }

  /**
   * If we want to test how the backend behaves with a non-valid message id we can set it by force
   */
  public void setMessageIdForced(String messageIdForced) {
    this.messageIdForced = messageIdForced;
  }
}
