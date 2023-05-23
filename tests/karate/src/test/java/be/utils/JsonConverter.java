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

  public JsonConverter(){
    this.publicKey = "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=";
  }

  public JsonConverter(String publicKey, String privateKey){
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  /**
   * Produces a valid Json representation of a message given the message data, the id of the message
   * and the channel where the message is supposed to be sent
   */
  public Json publishMessageFromData(String stringData, int id, String channel) {
    Json messageData = Json.object();
    messageData.set("method", "publish");
    messageData.set("id", id);
    Map<String, Object> paramsPart;
    try{
    paramsPart = constructParamsField(channel, stringData);
    messageData.set("params", paramsPart);
    }catch (GeneralSecurityException e){
      e.printStackTrace();
    }
    messageData.set("jsonrpc", "2.0");

    return messageData;
  }

  public Map<String, Object> constructMessageField(String stringData) throws GeneralSecurityException{
    Map<String, Object> messagePart = new LinkedHashMap<>();
    Json messageData = Json.of(stringData);
    String messageDataBase64 = Base64Utils.convertJsonToBase64(messageData);
    String signature = constructSignature(stringData);
    if(isSignatureForced){
      signature = this.signatureForced;
      isSignatureForced = false;
    }
    System.out.println("signature used was: " + signature);
    String messageId = Hash.hash(messageDataBase64.getBytes(), signature.getBytes());
    String[] witness = new String[0];

    messagePart.put("data", messageDataBase64);
    System.out.println("publicKey used was : " + publicKey);
    messagePart.put("sender", publicKey);
    messagePart.put("signature", signature);
    messagePart.put("message_id", messageId);
    System.out.println("message id is : " + messageId);
    messagePart.put("witness_signatures", witness);

    return messagePart;
  }

  public Map<String, Object> constructParamsField(String channel, String messageDataBase64) throws GeneralSecurityException {
    Map<String, Object> paramsPart = new LinkedHashMap<>();
    Map<String, Object> messagePart = constructMessageField(messageDataBase64);

    paramsPart.put("channel", channel);
    paramsPart.put("message", messagePart);

    return paramsPart;
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
